package com.binchencoder.skylb.metrics;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;

/**
 * A {@link ForwardingServerCall} which updates Prometheus metrics based on the server-side actions
 * taken for a single rpc, e.g., messages sent, latency, etc.
 */
class MetricsServerCall<R,S> extends ForwardingServerCall.SimpleForwardingServerCall<R,S> {
  private final Clock clock;
  private final GrpcMethod grpcMethod;
  private final ServerMetrics serverMetrics;
  private final Configuration configuration;
  private final Instant startInstant;

  MetricsServerCall(
      ServerCall<R,S> delegate,
      Clock clock,
      GrpcMethod grpcMethod,
      ServerMetrics serverMetrics,
      Configuration configuration) {
    super(delegate);
    this.clock = clock;
    this.grpcMethod = grpcMethod;
    this.serverMetrics = serverMetrics;
    this.configuration = configuration;
    this.startInstant = clock.instant();

    reportStartMetrics();
  }

  @Override
  public void close(Status status, Metadata responseHeaders) {
    reportEndMetrics(status);
    super.close(status, responseHeaders);
  }

  @Override
  public void sendMessage(S message) {
    if (grpcMethod.streamsResponses()) {
      serverMetrics.recordStreamMessageSent();
    }
    super.sendMessage(message);
  }

  private void reportStartMetrics() {
    serverMetrics.recordCallStarted();
  }

  private void reportEndMetrics(Status status) {
    serverMetrics.recordServerHandled(status.getCode());
    if (configuration.isIncludeLatencyHistograms()) {
      double latencySec = TimeUnit.MILLISECONDS.toSeconds(
          clock.millis() - startInstant.toEpochMilli());
      serverMetrics.recordLatency(latencySec);
    }
  }
}
