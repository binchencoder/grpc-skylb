package com.binchencoder.skylb.monitoring;

import com.binchencoder.skylb.SkyLbContext;
import com.binchencoder.skylb.config.MetricsConfig;
import com.binchencoder.util.StoppableTask;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

public abstract class AbstractProducer {

  protected final MetricsConfig metricsConfig;
  protected final Counter succeededMessageCount;
  protected final Meter succeededMessageMeter;
  protected final Counter failedMessageCount;
  protected final Meter failedMessageMeter;
  protected final Timer messagePublishTimer;
  protected final Timer messageLatencyTimer;
  protected final Counter messageLatencySloViolationCount;

  public AbstractProducer(SkyLbContext skyLbContext) {
    this.metricsConfig = skyLbContext.getMetricsConfig();

    Metrics metrics = skyLbContext.getMetrics();
    MetricRegistry metricRegistry = metrics.getRegistry();

    this.succeededMessageCount = metricRegistry
        .counter(metrics.metricName("messages", "succeeded"));
    this.succeededMessageMeter = metricRegistry
        .meter(metrics.metricName("messages", "succeeded", "meter"));
    this.failedMessageCount = metricRegistry.counter(metrics.metricName("messages", "failed"));
    this.failedMessageMeter = metricRegistry
        .meter(metrics.metricName("messages", "failed", "meter"));
    this.messagePublishTimer = metricRegistry
        .timer(metrics.metricName("message", "publish", "time"));
    this.messageLatencyTimer = metricRegistry
        .timer(metrics.metricName("message", "publish", "age"));
    this.messageLatencySloViolationCount = metricRegistry
        .counter(metrics.metricName("message", "publish", "age", "slo_violation"));
  }

  public StoppableTask getStoppableTask() {
    return null;
  }

  public Meter getFailedMessageMeter() {
    return this.failedMessageMeter;
  }
}
