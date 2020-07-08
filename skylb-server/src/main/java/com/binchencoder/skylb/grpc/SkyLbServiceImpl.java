package com.binchencoder.skylb.grpc;

import static com.binchencoder.skylb.hub.EndpointsHub.ChanCapMultiplication;
import static com.binchencoder.skylb.monitoring.PrometheusMetrics.NAMESPACE;
import static com.binchencoder.skylb.monitoring.PrometheusMetrics.SUBSYSTEM;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Maps;
import com.binchencoder.common.GoChannelQueue;
import com.binchencoder.skylb.config.AbstractConfig;
import com.binchencoder.skylb.hub.EndpointsHub;
import com.binchencoder.skylb.hub.EndpointsUpdate;
import com.binchencoder.skylb.hub.SkyLbGraph;
import com.binchencoder.skylb.lameduck.LameDuck;
import com.binchencoder.skylb.proto.ClientProtos.DiagnoseRequest;
import com.binchencoder.skylb.proto.ClientProtos.DiagnoseResponse;
import com.binchencoder.skylb.proto.ClientProtos.InstanceEndpoint;
import com.binchencoder.skylb.proto.ClientProtos.Operation;
import com.binchencoder.skylb.proto.ClientProtos.ReportLoadRequest;
import com.binchencoder.skylb.proto.ClientProtos.ReportLoadResponse;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ResolveResponse;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.binchencoder.skylb.proto.SkylbGrpc.SkylbImplBase;
import com.binchencoder.skylb.utils.GrpcContextUtils;
import com.google.common.base.Strings;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomUtils;
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
      .buckets(0, 0.1, 10)
      .register();

  private final EndpointsHub endpointsHub;
  private final LameDuck lameDuck;
  private final SkyLbGraph skyLbGraph;

  public static final Config config = new Config();
  public SkyLbServiceImpl(final EndpointsHub endpointsHub, final LameDuck lameDuck,
      final SkyLbGraph skyLbGraph) {
    this.endpointsHub = endpointsHub;
    this.lameDuck = lameDuck;
    this.skyLbGraph = skyLbGraph;
  }

  @Override
  public void resolve(ResolveRequest request, StreamObserver<ResolveResponse> responseObserver) {
    observeRpcCounts.inc();

    InetSocketAddress remoteAddr = (InetSocketAddress) GrpcContextUtils.getRemoteAddr();
    if (null == remoteAddr) {
      throw new StatusRuntimeException(
          Status.DATA_LOSS.withDescription("Failed to get peer client info from context."));
    }
    String hostString = remoteAddr.toString();
    LOGGER.debug("SkyLb server#resolve caller service {},  clientAddr: {}",
        request.getCallerServiceId(), hostString);

    List<ServiceSpec> specs = request.getServicesList();
    if (specs.isEmpty()) {
      throw new StatusRuntimeException(
          Status.INVALID_ARGUMENT.withDescription("No service spec found."));
    }
    // track service graph
    skyLbGraph.trackServiceGraph(request, remoteAddr);

    GoChannelQueue<EndpointsUpdate> endpointChannel;
    try {
      endpointChannel = endpointsHub
          .addObserver(specs, hostString, request.getResolveFullEndpoints());
    } catch (InterruptedException e) {
      for (ServiceSpec spec : specs) {
        addObserverFailCounts
            .labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName())).inc();
      }

      String log = String
          .format("Failed to register caller service ID %d client %s to observe services",
              request.getCallerServiceId(), remoteAddr.toString());
      LOGGER.error(log, e);
      responseObserver.onError(new StatusRuntimeException(Status.INTERNAL.withDescription(log)));
      return;
    }

    Timer timer = null;
    TimerTask disconnectTask = null;
    try {
      Map<String, Long> maxIds = Maps.newHashMap();
      for (ServiceSpec spec : specs) {
        maxIds.put(spec.getServiceName(), 0L);

        activeObserverGauge
            .labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName())).inc();
        LOGGER.info(
            "Registered caller service ID {} client {} to observe service {}.{} on port name {}",
            request.getCallerServiceId(), hostString,
            spec.getNamespace(), spec.getServiceName(), spec.getPortName());
      }

      timer = new Timer();
      disconnectTask = new TimerTask() {
        @Override
        public void run() {
          LOGGER.warn("Auto disconnect with client, clientAddr: {}", hostString);
          autoDisconnCounts.inc();

          endpointChannel.close(0);
        }
      };
      timer.schedule(disconnectTask, config.getFlagAutoDisconnTimeout()
          + RandomUtils.nextLong(0, config.getFlagAutoDisconnTimeout()));

      EndpointsUpdate eu;
      while (null != (eu = endpointChannel.take())) {
        notifyChanUsageHistogram.observe(
            (float) (endpointChannel.size()) / ChanCapMultiplication / (float) (request
                .getServicesCount()));

        long maxId = maxIds.get(eu.getEndpoints().getSpec().getServiceName());
        if (eu.getId() < maxId) {
          // Skip the old updates.
          continue;
        } else {
          maxIds.put(eu.getEndpoints().getSpec().getServiceName(), eu.getId());
        }

        ServiceEndpoints eps = eu.getEndpoints();
        this.logResolveEndpoints(request, remoteAddr, eps);

        ResolveResponse resp = ResolveResponse.newBuilder()
            .setSvcEndpoints(ServiceEndpoints.newBuilder()
                .setSpec(eps.getSpec())
                .addAllInstEndpoints(eps.getInstEndpointsList())
                .build())
            .build();

        final CountDownLatch respLatch = new CountDownLatch(1);
        try {
          responseObserver.onNext(resp);
          LOGGER.debug("responseObserver.onNext: clientAddr: {}, resp: {}", hostString,
              resp.toBuilder().build().toString());
        } catch (Throwable t) {
          String errMsg = String.format(
              "Failed to send endpoints update to caller service ID %d client %s, abandon the stream.",
              request.getCallerServiceId(), remoteAddr.getHostString());
          LOGGER.error(errMsg, t);
          responseObserver
              .onError(new StatusRuntimeException(Status.INTERNAL.withDescription(errMsg)));
        } finally {
          respLatch.countDown();
        }
        try {
          respLatch.await(config.getFlagNotifyTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
          // Discard the current gRPC stream if timeout.
          LOGGER.error(
              "Time out to send endpoints update to caller service ID {} client {}, abandon the stream.",
              request.getCallerServiceId(), remoteAddr.getHostString());
          // It's OK to record p.Addr.String in label value, since such events should be rare, and will not accumulate too much data.
          notifyTimeoutCounts
              .labels(request.getCallerServiceId().toString(), remoteAddr.getHostString()).inc();

          responseObserver.onError(new StatusRuntimeException(
              Status.fromCode(Code.DEADLINE_EXCEEDED)
                  .withDescription("time out to send endpoints update to client")));
        }
      }

      LOGGER.debug("responseObserver.onCompleted(), clientAddr: {}", hostString);
      responseObserver.onCompleted();
    } catch (InterruptedException e) {
      String errMsg = String.format(
          "Failed to send endpoints update to caller service ID %d client %s, abandon the stream.",
          request.getCallerServiceId(), hostString);
      LOGGER.error(errMsg, e);
      responseObserver.onError(new StatusRuntimeException(
          Status.INTERNAL.withDescription(errMsg)));
    } finally {
      if (null != disconnectTask) {
        disconnectTask.cancel();
      }
      if (null != timer) {
        timer.purge();
        timer.cancel();
      }

      this.removeObserver(request, remoteAddr, specs);
      // un track service graph
      skyLbGraph.untrackServiceGraph(request, remoteAddr);
    }
  }

  @Override
  public StreamObserver<ReportLoadRequest> reportLoad(
      StreamObserver<ReportLoadResponse> responseObserver) {
    reportLoadRpcCounts.inc();

    InetSocketAddress remoteAddr = (InetSocketAddress) GrpcContextUtils.getRemoteAddr();
    if (null == remoteAddr) {
      throw new StatusRuntimeException(
          Status.DATA_LOSS.withDescription("Failed to get peer client info from context."));
    }
    String clientHostAddr = remoteAddr.getAddress().getHostAddress();
    LOGGER.debug("SkyLb server#reportLoad Start accepting load report from {}.", clientHostAddr);

    activeReporterGauge.labels(clientHostAddr).inc();

    boolean[] first = {true};
    try {
      return new StreamObserver<ReportLoadRequest>() {
        @Override
        public void onNext(ReportLoadRequest req) {
          ServiceSpec spec = req.getSpec();
          String label = formatServiceSpec(spec.getNamespace(), spec.getServiceName());
          reportLoadCounts.labels(label).inc();

          // Replace host name if fixed_host has been specified.
          String fixHostAddr = clientHostAddr;
          if (!Strings.isNullOrEmpty(req.getFixedHost())) {
            fixHostAddr = req.getFixedHost();
            LOGGER.info("Use fixed host {} instead of {}", fixHostAddr, clientHostAddr);
          }

          if (first[0]) {
            LOGGER.info("Received init load report from {} at {}", label, remoteAddr.toString());
            initReportLoadCounts.labels(label).inc();

            /**
             * When the service with weights is turned off, the service is restarted in less than 10 seconds,
             * especially if the weights are modified. If only the epsHub.UpsertEndpoint method is used,
             * the weight level is not modified. purely Just to prevent this issue.
             */
            try {
              endpointsHub.insertEndpoint(spec, fixHostAddr, req.getPort(), req.getWeight());
            } catch (Exception e) {
              LOGGER.error(
                  "Failed to update etcd entry for endpoint {}:{}, closing the report stream.",
                  fixHostAddr, req.getPort());

              responseObserver.onError(e);
            }

            first[0] = false;
          }

          // Block the heart beat if the server is lame duck.
          String endpoint = lameDuck.formatHostPort(fixHostAddr, String.valueOf(req.getPort()));
          if (lameDuck.isLameduckMode(endpoint)) {
            LOGGER.info("Received load report from {}:{}, masked", fixHostAddr, req.getPort());
            return;
          }

          LOGGER.debug("Received load report from {}:{}.", fixHostAddr, req.getPort());
          try {
            endpointsHub
                .upsertEndpoint(req.getSpec(), fixHostAddr, req.getPort(), req.getWeight());
          } catch (Exception e) {
            LOGGER.error(
                "Failed to update etcd entry for endpoint {}:{}, closing the report stream.",
                fixHostAddr, req.getPort());

            responseObserver.onError(e);
          }
        }

        @Override
        public void onError(Throwable throwable) {
          if (throwable instanceof StatusRuntimeException) {
            StatusRuntimeException sre = (StatusRuntimeException) throwable;
            if (sre.getStatus().getCode() == Code.CANCELLED) {
              LOGGER.warn("ReportLoad close stream on client[{}]", remoteAddr.toString());
              return;
            }
          }
          LOGGER.error("ReportLoadRequest streamObserver error", throwable);
        }

        @Override
        public void onCompleted() {
          LOGGER.info("ReportLoadRequest streamObserver onCompleted");
          responseObserver.onCompleted();
        }
      };

    } finally {
      activeReporterGauge.labels(clientHostAddr).dec();
    }
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
    return String.format("%s.%s", nameSpace, serviceName);
  }

  private void removeObserver(ResolveRequest request, InetSocketAddress remoteAddr,
      List<ServiceSpec> specs) {
    LOGGER.info("Stop observing services for caller service ID {} client {}",
        request.getCallerServiceId(), remoteAddr.toString());
    endpointsHub.removeObserver(specs, remoteAddr.toString());
    for (ServiceSpec spec : specs) {
      activeObserverGauge
          .labels(this.formatServiceSpec(spec.getNamespace(), spec.getServiceName())).dec();
    }
  }

  private void logResolveEndpoints(ResolveRequest request, InetSocketAddress remoteAddr,
      ServiceEndpoints eps) {
    if (LOGGER.isDebugEnabled()) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < eps.getInstEndpointsCount(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        InstanceEndpoint iep = eps.getInstEndpointsList().get(i);
        sb.append(
            String.format("[%s]%s:%d", opToString(iep.getOp()), iep.getHost(), iep.getPort()));
      }
      if (request.getResolveFullEndpoints()) {
        LOGGER.debug("Full endpoints of service {} for caller service ID %d client {}: {}.",
            eps.getSpec().getServiceName(), request.getCallerServiceId(),
            remoteAddr.getHostString(), sb.toString());
      } else {
        LOGGER.debug("Endpoints changed for caller service ID {} client {} with updates {}.",
            request.getCallerServiceId(), remoteAddr.getHostString(), sb.toString());
      }
    } else {
      if (request.getResolveFullEndpoints()) {
        LOGGER.info("Send full endpoints of service {} for caller service ID {} client {}.",
            eps.getSpec().getServiceName(), request.getCallerServiceId(),
            remoteAddr.getHostString());
      } else {
        LOGGER.info("Endpoints changed for caller service ID {} client {}.",
            request.getCallerServiceId(), remoteAddr.getHostString());
      }
    }
  }

  @Parameters(separators = "=")
  public static class Config extends AbstractConfig {

    @Parameter(names = {"--auto-disconn-timeout", "-auto-disconn-timeout"},
        description = "The timeout to automatically disconnect the resolve RPC. e.g. 10s(10 Seconds), 10m(10 Minutes)")
    private Duration flagAutoDisconnTimeout = Duration.ofMinutes(5); // 5 minute

    @Parameter(names = {"--endpoints-notify-timeout", "-endpoints-notify-timeout"},
        description = "The timeout to notify client endpoints update. e.g. 10s(10 Seconds), 10m(10 Minutes)")
    private Duration flagNotifyTimeout = Duration.ofSeconds(10);

    public long getFlagAutoDisconnTimeout() {
      return flagAutoDisconnTimeout.toMillis();
    }

    public long getFlagNotifyTimeout() {
      return flagNotifyTimeout.toMillis();
    }

    @Override
    public String toKeyValues() {
      return new StringBuilder()
          .append("--auto-disconn-timeout").append("=")
          .append(this.flagAutoDisconnTimeout.toString()).append("\n")
          .append("--endpoints-notify-timeout").append("=")
          .append(this.flagNotifyTimeout.toString())
          .toString();
    }
  }
}
