package com.binchencoder.skylb.hub;

import static com.binchencoder.skylb.prometheus.PrometheusMetrics.NAMESPACE;
import static com.binchencoder.skylb.prometheus.PrometheusMetrics.SUBSYSTEM;

import com.beust.jcommander.Parameters;
import com.binchencoder.skylb.common.GoChannelPool;
import com.binchencoder.skylb.common.GoChannelPool.GoChannel;
import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.Endpoints;
import com.binchencoder.skylb.etcd.Endpoints.EndpointPort;
import com.binchencoder.skylb.etcd.Endpoints.EndpointSubset;
import com.binchencoder.skylb.etcd.Endpoints.EndpointSubset.EndpointAddress;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.hub.model.ClientObject;
import com.binchencoder.skylb.hub.model.ServiceEndpoint;
import com.binchencoder.skylb.hub.model.ServiceObject;
import com.binchencoder.skylb.proto.ClientProtos.InstanceEndpoint;
import com.binchencoder.skylb.proto.ClientProtos.Operation;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.prometheus.client.Gauge;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointsHubImpl implements EndpointsHub {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointsHubImpl.class);

  private static final AtomicLong atomicLong = new AtomicLong();

  private static final ReentrantReadWriteLock fairRWLock = new ReentrantReadWriteLock(true);

  // ConcurrentHashMap ??
  private Map<String, ServiceObject> services = new HashMap<>();
  private Map<String, String> graphKey = new ConcurrentHashMap();

  private static final Gauge addObserverGauge = Gauge.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("add_observer_gauge")
      .help("SkyLB add observer gauge.")
      .labelNames("service")
      .register();

  private static final Gauge removeObserverGauge = Gauge.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("remove_observer_gauge")
      .help("SkyLB remove observer gauge.")
      .labelNames("service")
      .register();

  private final EtcdClient etcdClient;

  private final ServerConfig serverConfig;

  private final GoChannelPool channelPool;

  // Constructor
  public EndpointsHubImpl(EtcdClient etcdClient, ServerConfig serverConfig,
      GoChannelPool channelPool) {
    this.etcdClient = etcdClient;
    this.serverConfig = serverConfig;
    this.channelPool = channelPool;
  }

  private ExecutorService endpointExecutor;

  public void registerProcessor(ExecutorService endpointExecutor) {
    this.endpointExecutor = endpointExecutor;
  }

  @Override
  public GoChannel<EndpointsUpdate> addObserver(List<ServiceSpec> specs, String clientAddr,
      Boolean resolveFull) {
    if (null == specs || specs.isEmpty()) {
      throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(""));
    }

    GoChannel<EndpointsUpdate> up = channelPool.newChannel(ChanCapMultiplication * specs.size());
    for (ServiceSpec spec : specs) {
      LOGGER.info("Resolve service {}.{} on port name {} from client {}", spec.getNamespace(),
          spec.getServiceName(), spec.getPortName(), clientAddr);
      addObserverGauge.labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName()))
          .inc();

      ClientObject co = new ClientObject();
      co.setServiceSpec(spec);
      co.setClientAddr(clientAddr);
      co.setResolveFull(resolveFull);

      Endpoints eps;
      if (serverConfig.isWithInK8s()) {
        // TODO(chenbin) implements it
        return null;
      } else {
        eps = this.fetchEndpoints(spec.getNamespace(), spec.getServiceName());
      }

      String key = etcdClient.calculateKey(spec.getNamespace(), spec.getServiceName());
      ServiceObject so;
      try {
        fairRWLock.writeLock().lock();
        LOGGER.info("Received initial endpoints for client {}: {}.", clientAddr, eps);

        Map<String, ServiceEndpoint> epsMap = this.skypbEndpointsToMap(spec, eps);
        if (services.containsKey(key)) {
          so = services.get(key);
        } else {
          so = new ServiceObject();
          so.setServiceSpec(spec);
          so.setEndpoints(epsMap);

          services.put(key, so);

          if (!serverConfig.isWithInK8s()) {
            // Periodically update the endpoints so that client gets a chance to rectify its endpoint list.
            endpointExecutor.submit(new Runnable() {
              @Override
              public void run() {
                new Timer(true).schedule(new TimerTask() {
                  @Override
                  public void run() {
                    LOGGER.info("Automatic endpoints rectification for {}.", key);
                    updateEndpoints(key);
                  }
                }, 0, serverConfig.getAutoRectifyInterval());
              }
            });
          }
        }

        up.offer(
            new EndpointsUpdate(atomicLong.getAndIncrement(), diffEndpoints(spec, null, epsMap)));
      } catch (Exception e) {
        try {
          up.close();
        } catch (Throwable throwable) {
          throwable.printStackTrace();
        }

        throw e;
      } finally {
        fairRWLock.writeLock().unlock();
      }

      try {
        so.getReadWriteLock().writeLock().lock();
        so.getObservers().add(co);
      } finally {
        so.getReadWriteLock().writeLock().unlock();
      }
    }

    return up;
  }

  @Override
  public void removeObserver(List<ServiceSpec> specs, String clientAddr) {

  }

  @Override
  public void insertEndpoint(ServiceSpec spec, String host, String port, Integer weight) {

  }

  @Override
  public void upsertEndpoint(ServiceSpec spec, String host, String port, Integer weight) {

  }

  @Override
  public void trackServiceGraph(ResolveRequest req, ServiceSpec callee, SocketAddress callerAddr) {

  }

  @Override
  public void untrackServiceGraph(ResolveRequest req, ServiceSpec callee,
      SocketAddress callerAddr) {

  }

  private Endpoints fetchEndpoints(String namespace,
      String serviceName) throws StatusRuntimeException {
    String key = etcdClient.calculateKey(namespace, serviceName);
    GetResponse resp;
    try {
      ByteSequence bytesKey = ByteSequence.from(key.getBytes());
      resp = etcdClient.getKvClient()
          .get(bytesKey, GetOption.newBuilder().withPrefix(bytesKey).build()).get();
    } catch (Exception e) {
      LOGGER.error("Etcd client get error, key: {}", key, e);
      throw new StatusRuntimeException(
          Status.UNAVAILABLE.withDescription("Etcd client get error, key:" + e));
    }

    if (resp.getCount() == 0) {
      LOGGER.warn("Service {}.{} absent, return empty list.", namespace, serviceName);
      return Endpoints.getDefaultInstance();
    }

    KeyValue kv = resp.getKvs().get(0);
    String v = kv.getValue().toString(Charset.defaultCharset());
    return new Gson().fromJson(v, Endpoints.class);
  }

  private Map<String, ServiceEndpoint> skypbEndpointsToMap(ServiceSpec spec, Endpoints eps) {
    Preconditions.checkArgument(null != eps, "Parameter of eps should be not null.");

    if (null == eps.getSubsets() || eps.getSubsets().size() == 0) {
      return Collections.EMPTY_MAP;
    }

    Map<String, ServiceEndpoint> map = Maps.newHashMapWithExpectedSize(eps.getSubsets().size());
    for (EndpointSubset s : eps.getSubsets()) {
      int port = this.findPort(s.getPorts(), spec.getPortName());
      if (port == 0) {
        continue;
      }
      for (EndpointAddress addr : s.getAddresses()) {
        ServiceEndpoint se = new ServiceEndpoint(addr.getIp(), port);
        String key = this.calculateWeightKey(addr.getIp(), port);
        if (eps.getLabels().containsKey(key)) {
          se.setWeight(Integer.valueOf(eps.getLabels().get(key)));
        }
        map.put(se.toString(), se);
      }
    }

    return map;
  }

  private ServiceEndpoints skypbEndpointsToSlice(ServiceSpec spec, Endpoints eps) {
    Preconditions.checkNotNull(eps, "Parameter of eps should be not null.");

    List<InstanceEndpoint> svcEps = Lists.newArrayListWithExpectedSize(eps.getSubsets().size());
    for (EndpointSubset s : eps.getSubsets()) {
      int port = this.findPort(s.getPorts(), spec.getPortName());
      if (port == 0) {
        continue;
      }

      for (EndpointAddress addr : s.getAddresses()) {
        InstanceEndpoint.Builder epBuilder = InstanceEndpoint.newBuilder()
            .setHost(addr.getIp())
            .setPort(port);
        String key = this.calculateWeightKey(addr.getIp(), port);
        if (eps.getLabels().containsKey(key)) {
          epBuilder.setWeight(Integer.valueOf(eps.getLabels().get(key)));
        }

        svcEps.add(epBuilder.build());
      }
    }

    return ServiceEndpoints.newBuilder()
        .setSpec(spec)
        .addAllInstEndpoints(svcEps)
        .build();
  }

  private int findPort(Set<EndpointPort> ports, String portName) {
    for (EndpointPort ep : ports) {
      if (ep.getName().equals(portName)) {
        return ep.getPort();
      }
    }
    return 0;
  }

  private ServiceEndpoints diffEndpoints(ServiceSpec spec, Map<String, ServiceEndpoint> last,
      Map<String, ServiceEndpoint> now) {
    // Found common entries.
    Set<String> commonKeys;
    if (null == last) {
      commonKeys = Collections.EMPTY_SET;
    } else {
      commonKeys = Sets.newHashSet(now.keySet());
      commonKeys.retainAll(last.keySet());
    }

    List<InstanceEndpoint> eps = Lists.newArrayList();
    // Found endpoints to be removed from client.
    if (null != last) {
      for (Map.Entry<String, ServiceEndpoint> entry : last.entrySet()) {
        if (!commonKeys.contains(entry.getKey())) {
          eps.add(InstanceEndpoint.newBuilder()
              .setOp(Operation.Delete)
              .setHost(entry.getValue().getIp())
              .setPort(entry.getValue().getPort())
              .build());
        }
      }
    }

    // Found endpoints to be added for client.
    for (Map.Entry<String, ServiceEndpoint> entry : now.entrySet()) {
      if (!commonKeys.contains(entry.getKey())) {
        eps.add(InstanceEndpoint.newBuilder()
            .setOp(Operation.Add)
            .setHost(entry.getValue().getIp())
            .setPort(entry.getValue().getPort())
            .setWeight(entry.getValue().getWeight())
            .build());
      }
    }

    return ServiceEndpoints.newBuilder()
        .setSpec(spec)
        .addAllInstEndpoints(eps)
        .build();
  }

  private void updateEndpoints(String key) {
    try {
      fairRWLock.readLock().lock();
      if (services.containsKey(key)) {
        LOGGER.warn("serviceObject nil for key {}", key);
        return;
      }

      ServiceObject so = services.get(key);
      try {
        this.fetchEndpoints(so.getServiceSpec().getNamespace(),
            so.getServiceSpec().getServiceName());
      } catch (StatusRuntimeException sre) {
        LOGGER.error("Failed to fetch endpoints for service {}.{}: {}",
            so.getServiceSpec().getNamespace(), so.getServiceSpec().getServiceName(), sre);
        return;
      }


    } finally {
      fairRWLock.readLock().unlock();
    }
  }

  private void applyEndpoints(ServiceObject so, Endpoints eps) {
    Preconditions.checkNotNull(so, "Parameter of so should be not null.");

    ServiceEndpoints fullEps;
    List<ClientObject> observers;
    try {
      so.getReadWriteLock().writeLock().lock();

      so.setEndpoints(skypbEndpointsToMap(so.getServiceSpec(), eps));
      fullEps = this.skypbEndpointsToSlice(so.getServiceSpec(), eps);
      observers = so.getObservers();
    } finally {
      so.getReadWriteLock().writeLock().unlock();
    }

    for (ClientObject observer : observers) {

    }
  }

  private String calculateWeightKey(String host, int port) {
    return new Formatter().format("%s_%d_weight", host, port).toString();
  }

  private String formatServiceSpec(String namespace, String serviceName) {
    return new Formatter().format("%s.%s", namespace, serviceName).toString();
  }

  @Parameters(separators = "=")
  public static class Config {

  }
}
