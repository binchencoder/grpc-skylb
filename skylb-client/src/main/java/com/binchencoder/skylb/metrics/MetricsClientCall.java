package com.binchencoder.skylb.metrics;

import java.time.Clock;

import io.grpc.ClientCall;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;

/**
 * A {@link SimpleForwardingClientCall} which increments monitoring counters for
 * the rpc call.
 */
class MetricsClientCall<R, S> extends ForwardingClientCall.SimpleForwardingClientCall<R, S> {
  // Be consistent with mdKeyClientName in /letsgo/grpc/metadata.go.
  public static final String MDKEY_CLIENT_NAME = "mdkey_client_name";
  public static final Metadata.Key<String> csNameHeadKey =
      Metadata.Key.of(MDKEY_CLIENT_NAME, Metadata.ASCII_STRING_MARSHALLER);

  private final ClientMetrics clientMetrics;
  private final GrpcMethod grpcMethod;
  private final Configuration configuration;
  private final Clock clock;

  MetricsClientCall(
      ClientCall<R, S> delegate,
      ClientMetrics clientMetrics,
      GrpcMethod grpcMethod,
      Configuration configuration,
      Clock clock) {
    super(delegate);
    this.clientMetrics = clientMetrics;
    this.grpcMethod = grpcMethod;
    this.configuration = configuration;
    this.clock = clock;
  }

  @Override
  public void start(ClientCall.Listener<S> delegate, Metadata metadata) {
    clientMetrics.recordCallStarted();
    metadata.put(csNameHeadKey, this.grpcMethod.callerServiceName());
    super.start(new MetricsClientCallListener<S>(
        delegate, clientMetrics, grpcMethod, configuration, clock), metadata);
  }

  @Override
  public void sendMessage(R requestMessage) {
    if (grpcMethod.streamsRequests()) {
      clientMetrics.recordStreamMessageSent();
    }
    super.sendMessage(requestMessage);
  }
}

