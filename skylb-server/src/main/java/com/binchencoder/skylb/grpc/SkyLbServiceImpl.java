package com.binchencoder.skylb.grpc;

import static com.binchencoder.skylb.prometheus.PrometheusMetrics.NAMESPACE;
import static com.binchencoder.skylb.prometheus.PrometheusMetrics.SUBSYSTEM;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Maps;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.hub.EndpointsHub;
import com.binchencoder.skylb.proto.ClientProtos.DiagnoseRequest;
import com.binchencoder.skylb.proto.ClientProtos.DiagnoseResponse;
import com.binchencoder.skylb.proto.ClientProtos.Operation;
import com.binchencoder.skylb.proto.ClientProtos.ReportLoadRequest;
import com.binchencoder.skylb.proto.ClientProtos.ReportLoadResponse;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ResolveResponse;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.binchencoder.skylb.proto.SkylbGrpc.SkylbImplBase;
import com.binchencoder.skylb.utils.GrpcContextUtils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import java.net.InetSocketAddress;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbServiceImpl extends SkylbImplBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbServiceImpl.class);

  private static final Gauge activeObserverGauge = Gauge.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("active_observer_gauge")
      .help("SkyLB active observer gauge.")
      .labelNames("service")
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

  private static final Counter reportLoadRpcCounts = Counter.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("report_load_rpc_counts")
      .help("SkyLB report load rpc counts.")
      .register();

  private static final Counter reportLoadCounts = Counter.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("report_load_counts")
      .help("SkyLB report load counts.")
      .labelNames("service")
      .register();

  private static final Counter initReportLoadCounts = Counter.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("init_report_load_counts")
      .help("SkyLB init report load counts.")
      .labelNames("service")
      .register();

  // To test this metric, enable flag:
  // --auto-disconn-timeout=2s
  private static final Counter autoDisconnCounts = Counter.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("auto_disconn_counts")
      .help("SkyLB auto disconnect counts.")
      .labelNames("service")
      .register();

  // To test this metric, enable flag:
  // --endpoints-notify-timeout=1ns
  private static final Counter notifyTimeoutCounts = Counter.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("notify_timeout_counts")
      .help("Notify client endpoints update timeout counts.")
      .labelNames("caller_service", "caller_addr")
      .register();

  private static final Histogram notifyChanUsageHistogram = Histogram.build()
      .namespace(NAMESPACE)
      .subsystem(SUBSYSTEM)
      .name("notify_chan_usage")
      .help("The usage rate of the notify channel.")
      .labelNames("caller_service", "caller_addr")
      .buckets(0, 0.1, 10)
      .register();

  public static Config config = new Config();

  private final EtcdClient etcdClient;
  private final EndpointsHub endpointsHub;

  public SkyLbServiceImpl(final EtcdClient etcdClient, final EndpointsHub endpointsHub) {
    this.etcdClient = etcdClient;
    this.endpointsHub = endpointsHub;
  }

  private ExecutorService serviceGraphExecutor;

  public void registerProcessor(ExecutorService serviceGraphExecutor) {
    this.serviceGraphExecutor = serviceGraphExecutor;
  }

  @Override
  public void resolve(ResolveRequest request, StreamObserver<ResolveResponse> responseObserver) {
    LOGGER.info("Caller service {}", request.getCallerServiceId());
    observeRpcCounts.inc();

    InetSocketAddress remoteAddr = (InetSocketAddress) GrpcContextUtils.getRemoteAddr();
    if (null == remoteAddr) {
      throw new StatusRuntimeException(
          Status.DATA_LOSS.withDescription("Failed to get peer client info from context."));
    }
    LOGGER.info("Remote ip: {}", remoteAddr.getHostString());

    List<ServiceSpec> specs = request.getServicesList();
    if (specs.isEmpty()) {
      throw new StatusRuntimeException(
          Status.INVALID_ARGUMENT.withDescription("No service spec found.."));
    }

    for (ServiceSpec spec : specs) {
      endpointsHub.trackServiceGraph(request, spec, remoteAddr);
    }

    try {
      try {
        endpointsHub.addObserver(specs, remoteAddr.getHostName(), request.getResolveFullEndpoints());
      } catch (Exception e) {
        for (ServiceSpec spec : specs) {
          addObserverFailCounts
              .labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName())).inc();
        }

        LOGGER.warn("Failed to register caller service ID {} client {} to observe services",
            request.getCallerServiceId(), remoteAddr.toString(), e);

        throw e;
      }

      Map<String, Long> maxIds = Maps.newHashMap();
      for (ServiceSpec spec : specs) {
        maxIds.put(spec.getServiceName(), 0L);

        activeObserverGauge
            .labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName())).inc();
        LOGGER.info(
            "Registered caller service ID {} client {} to observe service {}.{} on port name {}",
            request.getCallerServiceId(), remoteAddr.toString(), spec.getNamespace(),
            spec.getServiceName(), spec.getPortName());
      }
    } finally {
      // untrack service graph
      this.untrackServiceGraph(request, remoteAddr);

      LOGGER.info("Stop observing services for caller service ID {} client {}",
          request.getCallerServiceId(), remoteAddr.toString());
      endpointsHub.removeObserver(specs, remoteAddr.toString());
      for (ServiceSpec spec : specs) {
        endpointsHub.trackServiceGraph(request, spec, remoteAddr);
        activeObserverGauge
            .labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName())).dec();
      }
    }

    super.resolve(request, responseObserver);
  }

  @Override
  public StreamObserver<ReportLoadRequest> reportLoad(
      StreamObserver<ReportLoadResponse> responseObserver) {
    InetSocketAddress remoteAddr = (InetSocketAddress) GrpcContextUtils.getRemoteAddr();
    LOGGER.info("Remote ip: {}", remoteAddr.toString());

    return super.reportLoad(responseObserver);
  }

  @Override
  public StreamObserver<DiagnoseResponse> attachForDiagnosis(
      StreamObserver<DiagnoseRequest> responseObserver) {
    return super.attachForDiagnosis(responseObserver);
  }

  private String opToString(Operation op) {
    switch (op) {
      case Add:
        return "ADD";
      case Delete:
        return "DELETE";
      default:
        return "";
    }
  }

  private String formatServiceSpec(String nameSpace, String serviceName) {
    return new Formatter().format("%s.%s", nameSpace, serviceName).toString();
  }

  private void untrackServiceGraph(ResolveRequest request, InetSocketAddress remoteAddr) {
    serviceGraphExecutor.submit(() -> {
      for (ServiceSpec spec : request.getServicesList()) {
        endpointsHub.untrackServiceGraph(request, spec, remoteAddr);
      }
    });
  }

  @Parameters(separators = "=")
  public static class Config {

    @Parameter(names = {"--endpoints-notify-timeout", "-endpoints-notify-timeout"},
        description = "The timeout to notify client endpoints update in seconds.")
    private int endPointsNotifyTimeout = 10;

    @Parameter(names = {"--auto-disconn-timeout", "-auto-disconn-timeout"},
        description = "The timeout to automatically disconnect the resolve RPC in minutes.")
    private int flagAutoDisconnTimeout = 5;

    public int getEndPointsNotifyTimeout() {
      return endPointsNotifyTimeout;
    }

    public int getFlagAutoDisconnTimeout() {
      return flagAutoDisconnTimeout;
    }
  }
}
