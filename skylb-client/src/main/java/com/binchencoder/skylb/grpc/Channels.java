package com.binchencoder.skylb.grpc;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.binchencoder.skylb.Properties;
import com.binchencoder.skylb.balancer.consistenthash.ConsistentHashLoadBalancerFactory;
import com.binchencoder.skylb.healthcheck.CallOption;
import com.binchencoder.skylb.healthcheck.Sizer;
import com.binchencoder.skylb.healthcheck.SizerUser;
import com.binchencoder.skylb.metrics.Configuration;
import com.binchencoder.skylb.metrics.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

/**
 * Channels encapsulates the original grpc managed-channel ({@link #originChannel}),
 * and a decorated copy ({@link #channel}), which is enriched with some interceptors.
 *
 * <p/>
 * Beware: a background grpc health check routine is started by default when
 * Channels object is constructed, so when you want to close the channel, make
 * sure to call {@link #shutdown()}.
 *
 * <p/>
 * Alternatively, you can choose not to enable health check, yet set grpc idle
 * time to infinity, by providing jvm options:
 * <br/>
 * -Denable-health-check=false -Dgrpc-idle-timeout=2678400
 */
public class Channels implements SizerUser {
  static Logger logger = LoggerFactory.getLogger(Channels.class);

  private ManagedChannel originChannel;

  private Channel channel;

  private ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture<?> hFuture;

  Sizer sizer;
  String calleeServiceName;

  static final Counter healthCheckCounts = Counter.build()
      .namespace("skylb")
      .subsystem("client")
      .name("health_check_counts")
      .help("grpc health check counts.")
      .labelNames(Constants.CODE, Constants.GRPC_SERVICE)
      .register();

  static final Gauge healthCheckSuccessGauge = Gauge.build()
      .namespace("skylb")
      .subsystem("client")
      .name("health_check_success_gauge")
      .help("grpc health check success gauge.")
      .labelNames(Constants.GRPC_SERVICE)
      .register();

  static final Gauge healthCheckSuccessRate = Gauge.build()
      .namespace("skylb")
      .subsystem("client")
      .name("health_check_success_rate")
      .help("grpc health check success rate.")
      .labelNames(Constants.GRPC_SERVICE)
      .register();

  static final Histogram healthCheckLatency = Histogram.build()
      .namespace("skylb")
      .subsystem("client")
      .name("health_check_latency")
      .help("grpc health check latency.")
      .labelNames(Constants.GRPC_SERVICE)
      .buckets(Configuration.DEFAULT_LATENCY_BUCKETS)
      .register();

  public void setSizer(Sizer sizer) {
    this.sizer = sizer;
  }

  public void setCalleeServiceName(String calleeServiceName) {
    this.calleeServiceName = calleeServiceName;
  }

  public Channels(ManagedChannel originChannel, Channel channel) {
    this.originChannel = originChannel;
    this.channel = channel;

    this.scheduledExecutorService = Executors.newScheduledThreadPool(1,
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Channels-%d").build());

    if (Properties.enableHealthCheck) {
      startHealthCheck();
    }
  }

  void startHealthCheck() {
    logger.debug("Start health check with interval {} seconds", Properties.healthCheckIntervalInSec);
    this.hFuture = this.scheduledExecutorService.scheduleWithFixedDelay(
        new Runnable() {
          HealthGrpc.HealthBlockingStub stub = HealthGrpc.newBlockingStub(channel);
          HealthCheckRequest req = HealthCheckRequest.newBuilder().build();

          @Override
          public void run() {
            if (null == sizer) {
              // Try to check one, in case client code hasn't triggered grpc call yet.
              logger.debug("Sending initial health to {}", calleeServiceName);
              healthCheck(1);
              return;
            }

            int size = sizer.getSize();
            if (0 == size) {
              logger.error("The load balancer has empty instances of service {}", calleeServiceName);
              healthCheckSuccessGauge.labels(calleeServiceName).set(0);
              healthCheckSuccessRate.labels(calleeServiceName).set(0);
              return;
            }

            logger.debug("Sending health check for {} instances of service {}", size, calleeServiceName);
            healthCheck(size);
          }

          private void healthCheck(int size) {
            int success = 0;
            for (int i = 0; i < size; i++) {
              long start = System.currentTimeMillis();
              try {
                stub.withDeadlineAfter(Properties.healthCheckTimeoutInMillis, TimeUnit.MILLISECONDS)
                    .withOption(CallOption.HEALTH, "")
                    .withOption(ConsistentHashLoadBalancerFactory.HASHKEY, "") // To avoid warning in services like authz.
                    .check(req);
                healthCheckLatency.labels(calleeServiceName).observe((System.currentTimeMillis() - start) / 1000.0);
                healthCheckCounts.labels("OK", calleeServiceName).inc();
                ++success;
              } catch (StatusRuntimeException e) {
                Status.Code code = e.getStatus().getCode();
                if (isSafeError(code)) {
                  // Can be considered successful.
                  healthCheckLatency.labels(calleeServiceName).observe((System.currentTimeMillis() - start) / 1000.0);
                  ++success;
                }
                healthCheckCounts.labels(code.toString(), calleeServiceName).inc();
              } catch (Exception e) {
                healthCheckCounts.labels(Status.Code.UNKNOWN.toString(), calleeServiceName).inc();
                logger.error("Failed to send a health check for an instance of service {}", calleeServiceName, e);
              }
//              try {
//                Thread.sleep(1000); // fuyc: uncomment to verify health check working in correct order.
//              } catch (InterruptedException e) {
//                e.printStackTrace();
//              }
            }
            healthCheckSuccessGauge.labels(calleeServiceName).set(success);
            healthCheckSuccessRate.labels(calleeServiceName).set(success * 1.0 / size);
          }
        },
        5, Properties.healthCheckIntervalInSec, TimeUnit.SECONDS);
  }

  public static boolean isSafeError(Status.Code code) {
    return Status.Code.INVALID_ARGUMENT == code || Status.Code.UNIMPLEMENTED == code;
  }

  public ManagedChannel getOriginChannel() {
    return this.originChannel;
  }

  public void setOriginChannel(ManagedChannel originChannel) {
    this.originChannel = originChannel;
  }

  public Channel getChannel() {
    return this.channel;
  }

  public void setChannel(Channel channel) {
    this.channel = channel;
  }

  public void shutdown() {
    if (this.hFuture != null && !this.hFuture.isDone()) {
      this.hFuture.cancel(true);
    }

    if (this.scheduledExecutorService != null
        && !this.scheduledExecutorService.isShutdown()) {
      this.scheduledExecutorService.shutdownNow();
    }

    this.getOriginChannel().shutdown();
  }
}
