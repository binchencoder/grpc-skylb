package com.binchencoder.skylb.metrics;

import java.time.Clock;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * A {@link ServerInterceptor} which sends stats about incoming grpc calls to
 * Prometheus.
 */
public class MetricsServerInterceptor implements ServerInterceptor {
  private final Clock clock;
  private final Configuration configuration;
  private final ServerMetrics.Factory serverMetricsFactory;
  private final String calleeServiceName;

  public static MetricsServerInterceptor create(Configuration configuration,
                                                String calleeServiceName) {
    return new MetricsServerInterceptor(
        Clock.systemDefaultZone(), configuration, ServerMetrics.Factory.getInstance(),
        calleeServiceName);
  }

  private MetricsServerInterceptor(
      Clock clock, Configuration configuration,
      ServerMetrics.Factory serverMetricsFactory, String calleeServiceName) {
    this.clock = clock;
    this.configuration = configuration;
    this.serverMetricsFactory = serverMetricsFactory;
    this.calleeServiceName = calleeServiceName;
  }

  @Override
  public <R, S> ServerCall.Listener<R> interceptCall(
      ServerCall<R, S> call,
      Metadata requestHeaders,
      ServerCallHandler<R, S> next) {
    MethodDescriptor<R, S> method = call.getMethodDescriptor();
    String callerServiceName = requestHeaders.get(MetricsClientCall.csNameHeadKey);
    if (null == callerServiceName) {
      // Applies for before skylb-client 1.3.4-SNAPSHOT.
      callerServiceName = "UNKNOWN";
    }
    ServerMetrics metrics = serverMetricsFactory.createMetricsForMethod(method,
        callerServiceName, this.calleeServiceName);
    GrpcMethod grpcMethod = GrpcMethod.of(method, callerServiceName, this.calleeServiceName);
    ServerCall<R, S> monitoringCall = new MetricsServerCall<R, S>(
        call, clock, grpcMethod, metrics, configuration);
    return new MetricsServerCallListener<R>(
        next.startCall(monitoringCall, requestHeaders), metrics, grpcMethod);
  }
}
