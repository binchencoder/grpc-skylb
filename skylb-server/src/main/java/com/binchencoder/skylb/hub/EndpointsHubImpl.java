package com.binchencoder.skylb.hub;

import static com.binchencoder.skylb.prometheus.PrometheusMetrics.NAMESPACE;
import static com.binchencoder.skylb.prometheus.PrometheusMetrics.SUBSYSTEM;

import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.hub.model.ClientObject;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import java.net.InetAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointsHubImpl implements EndpointsHub {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointsHubImpl.class);

  private static final Gauge activeObserverGauge = Gauge.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("active_observer_gauge")
      .help("SkyLB active observer gauge.")
      .register();

  private static final Gauge activeReporterGauge = Gauge.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("active_reporter_gauge")
      .help("SkyLB active reporter gauge.")
      .labelNames("endpoint")
      .register();

  private static final Counter addObserverFailCounts = Counter.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("add_observer_fail_counts")
      .help("SkyLB observer rpc counts.")
      .labelNames("service")
      .register();

  private static final Counter observeRpcCounts = Counter.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("observe_rpc_counts")
      .help("SkyLB observer rpc counts.")
      .register();

  private final EtcdClient etcdClient;

  private final ServerConfig serverConfig;

  public EndpointsHubImpl(EtcdClient etcdClient, ServerConfig serverConfig) {
    this.etcdClient = etcdClient;
    this.serverConfig = serverConfig;
  }

  @Override
  public EndpointsUpdate addObserver(List<ServiceSpec> specs, String clientAddr,
      Boolean resolveFull) {
    if (null == specs || specs.isEmpty()) {
      throw new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription(""));
    }

    for (ServiceSpec spec : specs) {
      LOGGER.info("Resolve service {}.{} on port name {} from client {}", spec.getNamespace(),
          spec.getServiceName(), spec.getPortName(), clientAddr);

      ClientObject co = new ClientObject();
      co.setServiceSpec(spec);
      co.setClientAddr(clientAddr);
      co.setResolveFull(resolveFull);
    }

    return null;
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
  public void trackServiceGraph(ResolveRequest req, ServiceSpec callee, InetAddress callerAddr) {

  }

  @Override
  public void untrackServiceGraph(ResolveRequest req, ServiceSpec callee, InetAddress callerAddr) {

  }
}
