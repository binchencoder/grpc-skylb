package com.binchencoder.skylb.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.grpc.MethodDescriptor;
import io.grpc.Status.Code;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;

/**
 * Prometheus metric definitions used for client-side monitoring of grpc
 * services.
 */
class ClientMetrics {
  private static final Counter.Builder rpcStartedBuilder = Counter.build()
      .namespace("skylb")
      .subsystem("client")
      .name("started_total")
      .labelNames(Constants.GRPC_TYPE, Constants.SELF_SERVICE, Constants.GRPC_METHOD, Constants.GRPC_SERVICE)
      .help("Total number of RPCs started on the client.");

  private static final Counter.Builder rpcCompletedBuilder = Counter.build()
      .namespace("skylb")
      .subsystem("client")
      .name("completed")
      .labelNames(Constants.GRPC_TYPE, Constants.SELF_SERVICE, Constants.GRPC_METHOD, Constants.GRPC_SERVICE, Constants.CODE)
      .help("Total number of RPCs completed on the client, regardless of success or failure.");

  private static final Histogram.Builder completedLatencySecondsBuilder = Histogram.build()
      .namespace("skylb")
      .subsystem("client")
      .name("completed_latency_seconds")
      .labelNames(Constants.GRPC_TYPE, Constants.SELF_SERVICE, Constants.GRPC_METHOD, Constants.GRPC_SERVICE)
      .help("Histogram of rpc response latency (in seconds) for completed rpcs.");

  private static final Counter.Builder streamMessagesReceivedBuilder = Counter.build()
      .namespace("skylb")
      .subsystem("client")
      .name("msg_received_total")
      .labelNames(Constants.GRPC_TYPE, Constants.SELF_SERVICE, Constants.GRPC_METHOD, Constants.GRPC_SERVICE)
      .help("Total number of stream messages received from the server.");

  private static final Counter.Builder streamMessagesSentBuilder = Counter.build()
      .namespace("skylb")
      .subsystem("client")
      .name("msg_sent_total")
      .labelNames(Constants.GRPC_TYPE, Constants.SELF_SERVICE, Constants.GRPC_METHOD, Constants.GRPC_SERVICE)
      .help("Total number of stream messages sent by the client.");

  private final Counter rpcStarted;
  private final Counter rpcCompleted;
  private final Counter streamMessagesReceived;
  private final Counter streamMessagesSent;
  private final Optional<Histogram> completedLatencySeconds;

  private final GrpcMethod method;

  private ClientMetrics(GrpcMethod method, Counter rpcStarted, Counter rpcCompleted,
                        Counter streamMessagesReceived, Counter streamMessagesSent,
                        Optional<Histogram> completedLatencySeconds) {
    this.method = method;
    this.rpcStarted = rpcStarted;
    this.rpcCompleted = rpcCompleted;
    this.streamMessagesReceived = streamMessagesReceived;
    this.streamMessagesSent = streamMessagesSent;
    this.completedLatencySeconds = completedLatencySeconds;
  }

  public void recordCallStarted() {
    addLabels(rpcStarted).inc();
  }

  public void recordClientHandled(Code code) {
    addLabels(rpcCompleted, code.toString()).inc();
  }

  public void recordStreamMessageSent() {
    addLabels(streamMessagesSent).inc();
  }

  public void recordStreamMessageReceived() {
    addLabels(streamMessagesReceived).inc();
  }

  /**
   * Only has any effect if monitoring is configured to include latency
   * histograms. Otherwise, this does nothing.
   */
  public void recordLatency(double latencySec) {
    if (!completedLatencySeconds.isPresent()) {
      return;
    }
    addLabels(completedLatencySeconds.get()).observe(latencySec);
  }

  private <T> T addLabels(SimpleCollector<T> collector, String... labels) {
    List<String> allLabels = new ArrayList<String>();
    Collections.addAll(allLabels, method.type(), method.callerServiceName(),
        method.methodName(), method.serviceName());
    Collections.addAll(allLabels, labels);
    return collector.labels(allLabels.toArray(new String[0]));
  }

  /**
   * Knows how to produce {@link ClientMetrics} instances for individual
   * methods.
   */
  static class Factory {
    private static Factory instance;

    private final Counter rpcStarted;
    private final Counter rpcCompleted;
    private final Counter streamMessagesReceived;
    private final Counter streamMessagesSent;
    private final Optional<Histogram> completedLatencySeconds;
    private final Configuration configuration;

    private Factory(Configuration configuration) {
      CollectorRegistry registry = configuration.getCollectorRegistry();
      this.rpcStarted = rpcStartedBuilder.register(registry);
      this.rpcCompleted = rpcCompletedBuilder.register(registry);
      this.streamMessagesReceived = streamMessagesReceivedBuilder.register(registry);
      this.streamMessagesSent = streamMessagesSentBuilder.register(registry);
      this.configuration = configuration;

      if (this.configuration.isIncludeLatencyHistograms()) {
        this.completedLatencySeconds = Optional.of(ClientMetrics.completedLatencySecondsBuilder
            .buckets(this.configuration.getLatencyBuckets()).register(registry));
      } else {
        this.completedLatencySeconds = Optional.empty();
      }
    }

    public static synchronized Factory getInstance() {
      if (instance == null) {
        instance = new Factory(Configuration.allMetrics());
      }
      return instance;
    }

    /**
     * Creates a {@link ClientMetrics} for the supplied method.
     */
    <R, S> ClientMetrics createMetricsForMethod(MethodDescriptor<R, S> methodDescriptor,
                                                String callerServiceName, String calleeServiceName) {
      return new ClientMetrics(
          GrpcMethod.of(methodDescriptor, callerServiceName, calleeServiceName),
          rpcStarted,
          rpcCompleted,
          streamMessagesReceived,
          streamMessagesSent,
          completedLatencySeconds);
    }
  }
}
