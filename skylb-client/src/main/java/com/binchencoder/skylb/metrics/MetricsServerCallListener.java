package com.binchencoder.skylb.metrics;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

/**
 * A {@link ForwardingServerCallListener} which updates Prometheus metrics for a single rpc based
 * on updates received from grpc.
 */
class MetricsServerCallListener<R> extends ForwardingServerCallListener<R> {
  private final ServerCall.Listener<R> delegate;
  private final GrpcMethod grpcMethod;
  private final ServerMetrics serverMetrics;

  MetricsServerCallListener(
      ServerCall.Listener<R> delegate, ServerMetrics serverMetrics, GrpcMethod grpcMethod) {
    this.delegate = delegate;
    this.serverMetrics = serverMetrics;
    this.grpcMethod = grpcMethod;
  }

  @Override
  public void onMessage(R request) {
    if (grpcMethod.streamsRequests()) {
      serverMetrics.recordStreamMessageReceived();
    }
    super.onMessage(request);
  }

  @Override
  protected ServerCall.Listener<R> delegate() {
    return delegate;
  }
}
