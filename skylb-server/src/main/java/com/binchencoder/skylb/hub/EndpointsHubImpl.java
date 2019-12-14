package com.binchencoder.skylb.hub;

import static com.binchencoder.skylb.prometheus.PrometheusMetrics.NAMESPACE;
import static com.binchencoder.skylb.prometheus.PrometheusMetrics.SUBSYSTEM;

import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.Endpoints;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.hub.model.ClientObject;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.prometheus.client.Gauge;
import java.net.SocketAddress;
import java.util.Formatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointsHubImpl implements EndpointsHub {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointsHubImpl.class);

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
      addObserverGauge.labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName()))
          .inc();

      ClientObject co = new ClientObject();
      co.setServiceSpec(spec);
      co.setClientAddr(clientAddr);
      co.setResolveFull(resolveFull);

      Endpoints eps = null;
      if (serverConfig.isWithInK8s()) {
        // TODO(chenbin) implements it
      } else {

      }

      String key = etcdClient.calculateKey(spec.getNamespace(), spec.getServiceName());
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
  public void trackServiceGraph(ResolveRequest req, ServiceSpec callee, SocketAddress callerAddr) {

  }

  @Override
  public void untrackServiceGraph(ResolveRequest req, ServiceSpec callee,
      SocketAddress callerAddr) {

  }

  private String formatServiceSpec(String nameSpace, String serviceName) {
    return new Formatter().format("%s.%s", nameSpace, serviceName).toString();
  }
}
