package com.binchencoder.skylb.metrics;

import com.binchencoder.skylb.SkyLBUtils;

import java.time.Clock;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

/**
 * A {@link ClientInterceptor} which sends stats about incoming grpc calls to
 * Prometheus.
 */
public class MetricsClientInterceptor implements ClientInterceptor {
  private final Clock clock;
  private final Configuration configuration;
  private final ClientMetrics.Factory clientMetricsFactory;
  private final String calleeServiceName;
  private final String callerServiceName;

  /**
   * @param callerServiceName (only format will be checked)
   * @param calleeServiceName (only format will be checked)
   */
  public static MetricsClientInterceptor create(Configuration configuration,
                                                String calleeServiceName, String callerServiceName) {
    SkyLBUtils.checkServiceName(calleeServiceName);
    SkyLBUtils.checkServiceName(callerServiceName);
    return new MetricsClientInterceptor(
        Clock.systemDefaultZone(), configuration, ClientMetrics.Factory.getInstance(),
        calleeServiceName, callerServiceName);
  }

  private MetricsClientInterceptor(
      Clock clock,
      Configuration configuration,
      ClientMetrics.Factory clientMetricsFactory,
      String calleeServiceName, String callerServiceName) {
    this.clock = clock;
    this.configuration = configuration;
    this.clientMetricsFactory = clientMetricsFactory;
    this.calleeServiceName = calleeServiceName;
    this.callerServiceName = callerServiceName;
  }

  @Override
  public <R, S> ClientCall<R, S> interceptCall(
      MethodDescriptor<R, S> methodDesc, CallOptions callOptions, Channel channel) {
    ClientMetrics metrics = clientMetricsFactory.createMetricsForMethod(methodDesc, this.callerServiceName,
        this.calleeServiceName);
    return new MetricsClientCall<R, S>(
        channel.newCall(methodDesc, callOptions),
        metrics,
        GrpcMethod.of(methodDesc, this.callerServiceName, this.calleeServiceName),
        configuration,
        clock);
  }
}
