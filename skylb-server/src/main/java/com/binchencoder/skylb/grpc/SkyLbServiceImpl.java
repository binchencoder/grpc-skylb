package com.binchencoder.skylb.grpc;

import static com.binchencoder.skylb.hub.EndpointsHub.ChanCapMultiplication;
import static com.binchencoder.skylb.prometheus.PrometheusMetrics.NAMESPACE;
import static com.binchencoder.skylb.prometheus.PrometheusMetrics.SUBSYSTEM;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Maps;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.hub.EndpointsHub;
import com.binchencoder.skylb.hub.EndpointsUpdate;
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
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import java.net.InetSocketAddress;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbServiceImpl extends SkylbImplBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbServiceImpl.class);

  public static Config config = new Config();

  private final Random random = new Random();

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
    LOGGER.info("SkyLbServiceImpl#resolve caller service {}", request.getCallerServiceId());
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

    Timer timer = null;
    TimerTask timeoutTask = null;
    try {
      LinkedBlockingDeque<EndpointsUpdate> notiCh;
      try {
        notiCh = endpointsHub
            .addObserver(specs, remoteAddr.getHostName(), request.getResolveFullEndpoints());
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

      timer = new Timer();
      timeoutTask = new TimerTask() {
        @Override
        public void run() {
          LOGGER.info("Auto disconnect with client");
          autoDisconnCounts.inc();

          notiCh.clear();
          responseObserver.onCompleted();
        }
      };
      timer.schedule(timeoutTask,
          config.getFlagAutoDisconnTimeout() + random.nextInt(config.getFlagAutoDisconnTimeout()));

      EndpointsUpdate eu;
      while (null != (eu = notiCh.poll())) {
        LOGGER.info("SkyLb server #Resolve:  receive AddObserver notify chan {}.", eu.getId());
        notifyChanUsageHistogram.observe(
            (float) (notiCh.size()) / ChanCapMultiplication / (float) (request.getServicesCount()));

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
//                .addAllInstEndpoints(eps.getInstEndpointsList())
                .addInstEndpoints(InstanceEndpoint.newBuilder().setHost("127.0.0.1").setPort(11122))
                .build())
            .build();

        final CountDownLatch respLatch = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
          try {
            responseObserver.onNext(resp);
            LOGGER.info("responseObserver.onNext: {}", resp.toBuilder().toString());
          } catch (Throwable t) {
            String errMsg = new Formatter().format(
                "Failed to send endpoints update to caller service ID {} client {}, abandon the stream, {}.",
                request.getCallerServiceId(), remoteAddr.getHostString()).toString();
            responseObserver.onError(new StatusRuntimeException(
                Status.INTERNAL.withDescription(errMsg)));
          } finally {
            respLatch.countDown();
          }
        });
        try {
          respLatch.await(config.getFlagNotifyTimeout(), TimeUnit.SECONDS);
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

      responseObserver.onCompleted();
      LOGGER.info("responseObserver.onCompleted()");
    } finally {
      if (null != timeoutTask) {
        timeoutTask.cancel();
      }
      if (null != timer) {
        timer.purge();
        timer.cancel();
      }

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
        sb.append(new Formatter()
            .format("[%s]%s:%d", opToString(iep.getOp()), iep.getHost(), iep.getPort()));
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

    @Parameter(names = {"--auto-disconn-timeout", "-auto-disconn-timeout"},
        description = "The timeout to automatically disconnect the resolve RPC in seconds.")
    private int flagAutoDisconnTimeout = 5 * 60; // 5 minute

    @Parameter(names = {"--endpoints-notify-timeout", "-endpoints-notify-timeout"},
        description = "The timeout to notify client endpoints update in seconds.")
    private int flagNotifyTimeout = 10;

    public int getFlagAutoDisconnTimeout() {
      return flagAutoDisconnTimeout * 1000;
    }

    public int getFlagNotifyTimeout() {
      return flagNotifyTimeout;
    }
  }
}
