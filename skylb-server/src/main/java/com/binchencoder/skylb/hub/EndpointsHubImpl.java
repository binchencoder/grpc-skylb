package com.binchencoder.skylb.hub;

import static com.binchencoder.skylb.monitoring.PrometheusMetrics.NAMESPACE;
import static com.binchencoder.skylb.monitoring.PrometheusMetrics.SUBSYSTEM;
import static com.binchencoder.skylb.prefix.InitPrefix.ENDPOINTS_KEY;
import static com.binchencoder.skylb.prefix.InitPrefix.LAMEDUCK_KEY;

import com.beust.jcommander.Parameters;
import com.binchencoder.common.GoChannelQueue;
import com.binchencoder.common.ThreadFactoryImpl;
import com.binchencoder.skylb.config.AbstractConfig;
import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.Endpoints;
import com.binchencoder.skylb.etcd.Endpoints.EndpointPort;
import com.binchencoder.skylb.etcd.Endpoints.EndpointSubset;
import com.binchencoder.skylb.etcd.Endpoints.EndpointSubset.EndpointAddress;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.etcd.KeyUtil;
import com.binchencoder.skylb.hub.model.ClientObject;
import com.binchencoder.skylb.hub.model.ServiceEndpoint;
import com.binchencoder.skylb.hub.model.ServiceObject;
import com.binchencoder.skylb.lameduck.LameDuck;
import com.binchencoder.skylb.proto.ClientProtos.InstanceEndpoint;
import com.binchencoder.skylb.proto.ClientProtos.Operation;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.binchencoder.skylb.utils.PathUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch.Listener;
import io.etcd.jetcd.common.exception.ClosedClientException;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.prometheus.client.Gauge;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointsHubImpl implements EndpointsHub {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointsHubImpl.class);

  // Generate EndpointsUpdate id
  private final AtomicLong endpointsUpdateAtomicLong = new AtomicLong(1);

  private final ReentrantReadWriteLock fairRWLock = new ReentrantReadWriteLock(true);

  // ConcurrentHashMap ??
  private Map<String, ServiceObject> services = new HashMap<>();

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
  private LameDuck lameDuck;
  private final ServerConfig serverConfig;
  private final ExecutorService endpointExecutor;

  // Constructor
  public EndpointsHubImpl(EtcdClient etcdClient, LameDuck lameDuck, ServerConfig serverConfig)
      throws StatusRuntimeException {
    this.etcdClient = etcdClient;
    this.lameDuck = lameDuck;
    this.serverConfig = serverConfig;
    this.endpointExecutor = Executors
        .newCachedThreadPool(new ThreadFactoryImpl("EndpointExecutorThread_"));

    // Start watcher
    if (this.serverConfig.isWithInK8s()) {
      this.startK8sWatcher();
    } else {
      this.startMainWatcher();
    }

    // Start lame duck watcher
    this.startLameDuckWatcher();
  }

  @Override
  public GoChannelQueue<EndpointsUpdate> addObserver(List<ServiceSpec> specs,
      String clientAddr, Boolean resolveFull) throws InterruptedException {
    if (null == specs || specs.isEmpty()) {
      throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(""));
    }

    GoChannelQueue<EndpointsUpdate> up = new GoChannelQueue<>();
    for (ServiceSpec spec : specs) {
      LOGGER.info("Resolve service {}.{} on port name {} from client {}", spec.getNamespace(),
          spec.getServiceName(), spec.getPortName(), clientAddr);
      addObserverGauge.labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName()))
          .inc();

      ClientObject co = new ClientObject();
      co.setServiceSpec(spec);
      co.setClientAddr(clientAddr);
      co.setResolveFull(resolveFull);
      co.setNotifyChannel(up);

      Endpoints eps;
      if (serverConfig.isWithInK8s()) {
        // TODO(chenbin) implements it
        return null;
      } else {
        eps = this.fetchEndpoints(spec.getNamespace(), spec.getServiceName());
      }

      String key = KeyUtil.calculateEndpointKey(spec.getNamespace(), spec.getServiceName());
      ServiceObject so;
      try {
        this.fairRWLock.writeLock().lock();
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
            endpointExecutor.submit(() ->
                new Timer(true).schedule(new TimerTask() {
                  @Override
                  public void run() {
                    LOGGER.info("Automatic endpoints rectification for {}.", key);
                    updateEndpoints(key);
                  }
                }, 0, serverConfig.getAutoRectifyInterval())
            );
          }
        }

        up.offer(new EndpointsUpdate(endpointsUpdateAtomicLong.getAndIncrement(),
            this.diffEndpoints(spec, null, epsMap)));
      } catch (Exception e) {
        up.close(0);

        LOGGER.error("Offer to EndpointsUpdate channel error", e);
        throw e;
      } finally {
        this.fairRWLock.writeLock().unlock();
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
    Preconditions.checkArgument(null != specs && specs.size() > 0);
    for (ServiceSpec spec : specs) {
      String lable = this.formatServiceSpec(spec.getNamespace(), spec.getServiceName());
      removeObserverGauge.labels(lable).inc();

      String key = KeyUtil.calculateEndpointKey(spec.getNamespace(), spec.getServiceName());
      ServiceObject so;
      try {
        this.fairRWLock.readLock().lock();
        so = this.services.get(key);
      } finally {
        this.fairRWLock.readLock().unlock();
      }

      if (null == so) {
        return;
      }

      try {
        so.getReadWriteLock().writeLock().lock();
        so.setObservers(this.removeObserverFromSlice(so.getObservers(), spec, clientAddr));
      } finally {
        so.getReadWriteLock().writeLock().unlock();
      }
    }
  }

  @Override
  public void insertEndpoint(ServiceSpec spec, String host, int port, Integer weight) {
    String key = KeyUtil.calculateEndpointKey(spec.getNamespace(), spec.getServiceName(), host,
        Integer.valueOf(port));

    try {
      etcdClient.setEndpointsKey(key, spec, host, port, weight);
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error("EtcdClient#setEndpointsKey error", e);
    }
  }

  @Override
  public void upsertEndpoint(ServiceSpec spec, String host, int port, Integer weight) {
    String key = KeyUtil.calculateEndpointKey(spec.getNamespace(), spec.getServiceName(), host,
        Integer.valueOf(port));

    try {
      etcdClient.refreshKey(key);
    } catch (EtcdException etcdEx) {
      // Sometimes the key might be dropped or expired so that refreshKey will fail.
      if (etcdEx.getErrorCode() == ErrorCode.DATA_LOSS) {
        this.insertEndpoint(spec, host, port, weight);
      }
    } catch (ExecutionException | InterruptedException e) {
      LOGGER.error("EtcdClient#refresh key error", e);
    }
  }

  @Override
  public void updateEndpoints(String key) {
    if (key.isEmpty()) {
      return;
    }

    ServiceObject so;
    try {
      fairRWLock.readLock().lock();
      so = services.get(key);
    } finally {
      fairRWLock.readLock().unlock();
    }

    if (null == so) {
      LOGGER.warn("serviceObject nil for key [{}]", key);
      return;
    }

    try {
      Endpoints eps = this.fetchEndpoints(so.getServiceSpec().getNamespace(),
          so.getServiceSpec().getServiceName());
      this.applyEndpoints(so, eps);
    } catch (StatusRuntimeException sre) {
      LOGGER.error("Failed to fetch endpoints for service {}.{}: {}",
          so.getServiceSpec().getNamespace(), so.getServiceSpec().getServiceName(), sre);
    }
  }

  @Override
  public void close() throws Exception {
    Optional.ofNullable(endpointExecutor).ifPresent(ExecutorService::shutdown);
    LOGGER.info("Shutting down endpointExecutor ...");
  }

  private void startK8sWatcher() throws StatusRuntimeException {
    // TODO(chenbin) implement it with in k8s
  }

  private void startMainWatcher() throws StatusRuntimeException {
    new Thread(this::runMainWatcher).start();
  }

  private void runMainWatcher() throws StatusRuntimeException {
    final ByteSequence bytesKey = ByteSequence.from(ENDPOINTS_KEY.getBytes());
    // Watch etcd keys for all service endpoints and notify clients.
    Listener listener = new Listener() {
      @Override
      public void onNext(WatchResponse response) {
        // Watch etcd keys for all service endpoints and notify clients.
        LOGGER.info("OnNext: watch endpoints key[{}], response:{}", ENDPOINTS_KEY, response);

        List<WatchEvent> events = response.getEvents();
        for (WatchEvent event : events) {
          extractUpdates(event);
        }
      }

      @Override
      public void onError(Throwable throwable) {
        LOGGER.error("OnError: watch endpoints key[{}]", ENDPOINTS_KEY, throwable);

        runMainWatcher();
      }

      @Override
      public void onCompleted() {
        LOGGER.info("OnCompleted: watch endpoints key[{}]", ENDPOINTS_KEY);
      }
    };

    try {
      this.etcdClient.getWatchClient()
          .watch(bytesKey, WatchOption.newBuilder().withPrefix(bytesKey).build(), listener);
    } catch (ClosedClientException cce) {
      try {
        Thread.currentThread().sleep(1000l);
      } catch (InterruptedException e) {
        // Ignore error
      }

      LOGGER.error("Abandon watcher", cce);
    }
  }

  // Starts a watcher to watch changes of lame duck.
  private void startLameDuckWatcher() throws StatusRuntimeException {
    new Thread(this::runLameDuckWatcher).start();
  }

  private void runLameDuckWatcher() throws StatusRuntimeException {
    final ByteSequence bytesKey = ByteSequence.from(LAMEDUCK_KEY.getBytes());

    GetResponse resp;
    try {
      resp = etcdClient.getKvClient()
          .get(bytesKey, GetOption.newBuilder().withPrefix(bytesKey).build()).get();
      this.lameDuck.extractLameduck(resp);
    } catch (Throwable throwable) {
      if (throwable.getCause().getCause() instanceof StatusRuntimeException) {
        // Failed to connect etcd
        StatusRuntimeException sre = (StatusRuntimeException) throwable.getCause().getCause();
        if (sre.getStatus().getCode() == Code.UNAVAILABLE) {
          LOGGER.error("Failed to connect etcd, will system out.", throwable);

          throw sre;
//          System.exit(-3);
        }
      }

      LOGGER.error("Failed to load lameduck instances with key prefix {}", LAMEDUCK_KEY, throwable);
    }

    // Load current lameduck endpoints.
    Listener listener = new Listener() {
      @Override
      public void onNext(WatchResponse response) {
        // Watch etcd keys for all service endpoints and notify clients.
        LOGGER.info("OnNext: watch lameduck changed key[{}], response: {}", LAMEDUCK_KEY, response);

        if (response.getEvents().size() == 0) {
          return;
        }
        lameDuck.extractLameduckChange(response.getEvents());
      }

      @Override
      public void onError(Throwable throwable) {
        LOGGER.error("OnError: watch lameduck key[{}]", LAMEDUCK_KEY, throwable);

        runLameDuckWatcher();
      }

      @Override
      public void onCompleted() {
        LOGGER.info("OnCompleted: watch lameduck key[{}]", LAMEDUCK_KEY);
      }
    };

    try {
      this.etcdClient.getWatchClient()
          .watch(bytesKey, WatchOption.newBuilder().withPrefix(bytesKey).build(), listener);
    } catch (ClosedClientException cce) {
      try {
        Thread.currentThread().sleep(1000l);
      } catch (InterruptedException e) {
        // Ignore error
      }

      LOGGER.error("Abandon watcher", cce);
    }
  }

  private void extractUpdates(WatchEvent event) {
    String key = "";
    switch (event.getEventType()) {
      case PUT:
        key = PathUtil.getPathPart(event.getKeyValue().getKey().toString(Charset.defaultCharset()));
        break;
      case DELETE:
        key = PathUtil.getPathPart(event.getPrevKV().getKey().toString(Charset.defaultCharset()));
        break;
      default:
        LOGGER.error("Unexpected action {}, ignore.", event.getEventType().name());
        break;
    }

    this.updateEndpoints(key);
  }

  private Endpoints fetchEndpoints(String namespace,
      String serviceName) throws EtcdException {
    String key = KeyUtil.calculateEndpointKey(namespace, serviceName);
    GetResponse resp;
    try {
      ByteSequence bytesKey = ByteSequence.from(key.getBytes());
      resp = etcdClient.getKvClient()
          .get(bytesKey, GetOption.newBuilder().withPrefix(bytesKey).build()).get();
    } catch (Exception e) {
      LOGGER.error("Etcd client get key error, key: {}", key, e);
      throw EtcdExceptionFactory
          .newEtcdException(ErrorCode.UNAVAILABLE, "Etcd client get error, key:" + e);
    }

    if (resp.getCount() == 0) {
      LOGGER.warn("Service {}.{} absent, return empty list.", namespace, serviceName);
      return Endpoints.getDefaultInstance();
    }

    Endpoints endpoints;
    List<KeyValue> kvs = resp.getKvs();
    if (kvs.size() > 1) {
      endpoints = Endpoints.newBuilder().build();
      for (int i = 0; i < kvs.size(); i++) {
        String v = kvs.get(i).getValue().toString(Charset.defaultCharset());
        if (Strings.isNullOrEmpty(v)) {
          continue;
        }

        Endpoints eps = this.unmarshalEndpoints(v);
        if (i == 0) {
          endpoints.setName(eps.getName());
          endpoints.setNamespace(eps.getNamespace());
//          endpoints.ObjectMeta = eps.ObjectMeta
        }

        for (Entry<String, String> e : eps.getLabels().entrySet()) {
          if (null == endpoints.getLabels() || endpoints.getLabels().isEmpty()) {
            endpoints.setLabels(Maps.newHashMap());
          }
          endpoints.getLabels().put(e.getKey(), e.getValue());
        }
        endpoints.getSubsets().addAll(eps.getSubsets());
      }
    } else {
      KeyValue kv = resp.getKvs().get(0);
      String v = kv.getValue().toString(Charset.defaultCharset());
      if (Strings.isNullOrEmpty(v)) {
        endpoints = Endpoints.newBuilder().build();
      } else {
        endpoints = this.unmarshalEndpoints(v);
      }
    }

    return endpoints;
  }

  private Endpoints unmarshalEndpoints(String v) {
    try {
      return new Gson().fromJson(v, Endpoints.class);
    } catch (JsonSyntaxException e) {
      LOGGER.error("unmarshal endpoints error, v: {}", v, e);
      throw EtcdExceptionFactory
          .newEtcdException(ErrorCode.INTERNAL, "unmarshal endpoints error: " + e.getMessage());
    }
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
        String key = KeyUtil.calculateWeightKey(addr.getIp(), port);
        if (null != eps.getLabels() && eps.getLabels().containsKey(key)) {
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
        String key = KeyUtil.calculateWeightKey(addr.getIp(), port);
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
      endpointExecutor.submit(() -> {
        if (fullEps.getInstEndpointsCount() == 0) {
          return;
        }

        EndpointsUpdate up = new EndpointsUpdate(endpointsUpdateAtomicLong.getAndIncrement(),
            fullEps);
        try {
          observer.getNotifyChannel().offer(up);
        } catch (InterruptedException e) {
          // Ignore error
        }
      });
    }
  }

  private List<ClientObject> removeObserverFromSlice(List<ClientObject> obs, ServiceSpec spec,
      String clientAddr) {
    List<ClientObject> remaining = Lists.newArrayListWithExpectedSize(obs.size());

    for (ClientObject ob : obs) {
      if (ob.getClientAddr().equals(clientAddr) && ob.getServiceSpec().toByteString()
          .equals(spec.toByteString())) {
        // Stop all goroutines for this observer.
        ob.getNotifyChannel().close(0);
      } else {
        remaining.add(ob);
      }
    }

    return remaining;
  }

  private String formatServiceSpec(String namespace, String serviceName) {
    return String.format("%s.%s", namespace, serviceName);
  }

  @Parameters(separators = "=")
  public class Config extends AbstractConfig {

    @Override
    public String toKeyValues() {
      return "";
    }
  }
}
