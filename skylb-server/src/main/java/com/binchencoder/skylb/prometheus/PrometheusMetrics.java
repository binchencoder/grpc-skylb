package com.binchencoder.skylb.prometheus;

import io.prometheus.client.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility functions for prometheus.
 */
public class PrometheusMetrics {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusMetrics.class);

  public static final String NAMESPACE = "skylb";
  public static final String SUBSYSTEM = "server";

  /**
   * Prometheus metric of failure count increment.
   *
   * @param labelValues metric label values
   */
  public static void failureCountInc(Counter failureCounter, String... labelValues) {
    try {
      failureCounter.labels(labelValues).inc();
    } catch (Exception e) {
      LOGGER.error("Failure counter increment error", e);
    }
  }
}
