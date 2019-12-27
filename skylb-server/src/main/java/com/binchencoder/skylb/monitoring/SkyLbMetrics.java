package com.binchencoder.skylb.monitoring;

import com.binchencoder.skylb.config.MetricsConfig;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.base.Strings;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbMetrics implements Metrics {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbMetrics.class);

  static final String reportingTypeSlf4j = "slf4j";
  static final String reportingTypeJmx = "jmx";
  static final String reportingTypeHttp = "http";

  private final MetricsConfig config;
  private final MetricRegistry metricRegistry;
  private String metricsPrefix;

  public SkyLbMetrics(MetricsConfig config) {
    this.config = config;
    this.metricRegistry = config.metricRegistry;
    this.setup(config);
  }

  public String metricName(String... names) {
    return MetricRegistry.name(metricsPrefix, names);
  }

  @Override
  public MetricRegistry getRegistry() {
    return this.metricRegistry;
  }

  @Override
  public <T extends Metric> void register(String name, T metric) throws IllegalArgumentException {
    getRegistry().register(name, metric);
  }

  private void setup(MetricsConfig config) {
    metricsPrefix = config.getMetricsPrefix();

    if (Strings.isNullOrEmpty(config.getMetricsType())) {
      LOGGER.warn("Metrics will not be exposed: metricsReportingType not configured.");
      return;
    }

    if (config.isMetricsJvm()) {
      this.metricRegistry.register(metricName("jvm", "memory_usage"), new MemoryUsageGaugeSet());
      this.metricRegistry.register(metricName("jvm", "gc"), new GarbageCollectorMetricSet());
      this.metricRegistry
          .register(metricName("jvm", "class_loading"), new ClassLoadingGaugeSet());
      this.metricRegistry
          .register(metricName("jvm", "file_descriptor_ratio"), new FileDescriptorRatioGauge());
      this.metricRegistry.register(metricName("jvm", "thread_states"),
          new CachedThreadStatesGaugeSet(60, TimeUnit.SECONDS));
    }

    if (config.getMetricsType().contains(reportingTypeSlf4j)) {
      final Slf4jReporter reporter = Slf4jReporter.forRegistry(this.metricRegistry)
          .outputTo(LOGGER)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();

      reporter.start(config.getMetricsSlf4jInterval().getSeconds(), TimeUnit.SECONDS);
      LOGGER.info("Slf4j metrics reporter enabled");
    }

    if (config.getMetricsType().contains(reportingTypeJmx)) {
      final JmxReporter jmxReporter = JmxReporter.forRegistry(config.metricRegistry)
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();
      jmxReporter.start();
      LOGGER.info("JMX metrics reporter enabled");

      if (System.getProperty("com.sun.management.jmxremote") == null) {
        LOGGER.warn("JMX remote is disabled");
      } else {
        String portString = System.getProperty("com.sun.management.jmxremote.port");
        if (portString != null) {
          LOGGER.info("JMX running on port {}", Integer.parseInt(portString));
        }
      }
    }

    if (config.getMetricsType().contains(reportingTypeHttp)) {
      CollectorRegistry.defaultRegistry.register(new DropwizardExports(this.metricRegistry));
    }
  }

  static class Registries {

    final MetricRegistry metricRegistry;
    final HealthCheckRegistry healthCheckRegistry;

    Registries(MetricRegistry metricRegistry, HealthCheckRegistry healthCheckRegistry) {
      this.metricRegistry = metricRegistry;
      this.healthCheckRegistry = healthCheckRegistry;
    }
  }
}
