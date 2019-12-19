package com.binchencoder.skylb.hub;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.binchencoder.skylb.common.ThreadFactoryImpl;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbGraphImpl implements SkyLbGraph {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbGraphImpl.class);

  public static Config config = new Config();

  private Timer timer;

  private Map<String, String> graphKey = new ConcurrentHashMap();

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
//        endpointsHub.untrackServiceGraph(request, spec, remoteAddr);
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
        // Clone the graph keys map.
        Set<String> keys = graphKey.keySet();


      }
    }, config.getGraphKeyInterval());
  }

  @Parameters(separators = "=")
  public static class Config {

    @Parameter(names = {"--graph-key-ttl", "-graph-key-ttl"},
        description = "The service graph key TTL in hours.")
    private int graphKeyTtl = 24;

    @Parameter(names = {"--graph-key-interval", "-graph-key-interval"},
        description = "The service graph key update interval in hours.")
    private int graphKeyInterval = 2;

    public int getGraphKeyTtl() {
      return graphKeyTtl;
    }

    public int getGraphKeyInterval() {
      return graphKeyInterval * 60 * 1000;
    }
  }
}
