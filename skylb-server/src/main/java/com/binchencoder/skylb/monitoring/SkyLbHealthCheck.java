package com.binchencoder.skylb.monitoring;

import com.codahale.metrics.Meter;
import com.codahale.metrics.health.HealthCheck;

public class SkyLbHealthCheck extends HealthCheck {

  private final Meter failedMessageMeter;

  public SkyLbHealthCheck(AbstractProducer producer) {
    if (null != producer) {
      this.failedMessageMeter = producer.getFailedMessageMeter();
    } else {
      this.failedMessageMeter = null;
    }
  }

  @Override
  protected Result check() throws Exception {
    // TODO: this should be configurable.
    if (failedMessageMeter != null && failedMessageMeter.getFifteenMinuteRate() > 0) {
      return Result.unhealthy(">1 messages failed to be sent to Kafka in the past 15minutes");
    } else {
      return Result.healthy();
    }
  }
}
