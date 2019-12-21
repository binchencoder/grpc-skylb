package com.binchencoder.skylb.hub;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.binchencoder.skylb.common.ThreadFactoryImpl;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.etcd.KeyUtil;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import io.etcd.jetcd.ByteSequence;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbGraphImpl implements SkyLbGraph {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbGraphImpl.class);

  public static Config config = new Config();

  private Timer timer;

  private Map<String, String> graphKey = new ConcurrentHashMap();
  private final ReentrantReadWriteLock graphKeyLock = new ReentrantReadWriteLock();

  private final EtcdClient etcdClient;
  private final ExecutorService serviceGraphExecutor;

  // Constructor
  public SkyLbGraphImpl(EtcdClient etcdClient) {
    this.etcdClient = etcdClient;

    this.serviceGraphExecutor = Executors
        .newSingleThreadExecutor(new ThreadFactoryImpl("ServiceGraphExecutorThread_"));

    this.startGraphTracking();
  }

  @Override
  public void trackServiceGraph(ResolveRequest req, ServiceSpec callee, SocketAddress callerAddr) {
    LOGGER.info("TrackServiceGraph {}|{} --> {}", req.getCallerServiceId(),
        req.getCallerServiceName(), callee);

    String graphKey = KeyUtil
        .calculateClientGraphKey(callee.getNamespace(), callee.getServiceName(),
            req.getCallerServiceName());
    LOGGER.info("etcd set graph key[{}]", graphKey);

    try {
      this.graphKeyLock.writeLock().lock();
      this.graphKey.put(graphKey, "");
    } finally {
      this.graphKeyLock.writeLock().unlock();
    }

    long nowSeconds = System.currentTimeMillis() / 1000;
    boolean succeeded = false;
    for (int i = 0; i < 3; i++) {
      try {
        etcdClient.setKeyWithTtl(ByteSequence.from(graphKey.getBytes()),
            ByteSequence.from(String.valueOf(nowSeconds).getBytes()),
            config.getGraphKeyTtl());
      } catch (ExecutionException | InterruptedException e) {
        if (i == 2) {
          LOGGER.warn("Save service graph key[{}] in etcd, error: ", graphKey, e);
        }
        continue;
      }

      succeeded = true;
      break;
    }

    if (!succeeded) {
      LOGGER.error("Failed to save service graph key[{}] in etcd for 3 times.", graphKey);
    }
  }

  @Override
  public void trackServiceGraph(ResolveRequest req, SocketAddress callerAddr) {
    for (ServiceSpec spec : req.getServicesList()) {
      this.trackServiceGraph(req, spec, callerAddr);
    }
  }

  @Override
  public void untrackServiceGraph(ResolveRequest req, SocketAddress callerAddr) {
    serviceGraphExecutor.submit(() -> {
      for (ServiceSpec spec : req.getServicesList()) {
        LOGGER.info("UntrackServiceGraph {}|{} --> {}", req.getCallerServiceId(),
            req.getCallerServiceName(), spec);
        String graphKey = KeyUtil
            .calculateClientGraphKey(spec.getNamespace(), spec.getServiceName(),
                req.getCallerServiceName());
        try {
          this.graphKeyLock.writeLock().lock();
          this.graphKey.remove(graphKey);
        } finally {
          this.graphKeyLock.writeLock().unlock();
        }
      }
    });
  }

  @Override
  public void close() throws Exception {
    Optional.ofNullable(serviceGraphExecutor).ifPresent(ExecutorService::shutdown);
    LOGGER.info("Shutting down serviceGraphExecutor ...");

    if (null != this.timer) {
      this.timer.cancel();
      LOGGER.info("Canceling graph track timer ...");
    }
  }

  private void startGraphTracking() {
    this.timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        // 只能在没有写锁的情况下读
        Set<String> keys;
        try {
          graphKeyLock.readLock().lock();
          keys = graphKey.keySet();
        } finally {
          graphKeyLock.readLock().unlock();
        }

        if (!keys.isEmpty()) {
          long nowSeconds = System.currentTimeMillis() / 1000;
          for (String key : keys) {
            try {
              etcdClient.setKeyWithTtl(ByteSequence.from(key.getBytes()),
                  ByteSequence.from(String.valueOf(nowSeconds).getBytes()),
                  config.getGraphKeyTtl());
            } catch (ExecutionException | InterruptedException e) {
              LOGGER.warn("Save service graph key {} in etcd err:", key, e);
            }

            // Throttle the traffic to ETCD to 20 keys/sec.
            try {
              Thread.currentThread().sleep(50);
            } catch (InterruptedException e) {
              // Ignore error
            }
          }
        }
      }
    }, config.getGraphKeyInterval());
  }

  @Parameters(separators = "=")
  public static class Config {

    @Parameter(names = {"--graph-key-ttl", "-graph-key-ttl"},
        description = "The service graph key TTL. e.g. 10m(10 Minutes), 24h(24Hours)")
    private Duration graphKeyTtl = Duration.ofHours(24);

    @Parameter(names = {"--graph-key-interval", "-graph-key-interval"},
        description = "The service graph key update interval. e.g. 10m(10 Minutes), 24h(24Hours)")
    private Duration graphKeyInterval = Duration.ofHours(2);

    public long getGraphKeyTtl() {
      return graphKeyTtl.toMillis();
    }

    public long getGraphKeyInterval() {
      return graphKeyInterval.toMillis();
    }
  }
}
