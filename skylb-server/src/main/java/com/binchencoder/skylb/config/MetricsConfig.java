package com.binchencoder.skylb.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Objects;

@Parameters(separators = "=",
    commandNames = {"metrics"}, commandDescription = "Help for skylb metrics: ")
public class MetricsConfig extends AbstractConfig {

  @Parameter(names = {"--metrics-prefix", "-metrics_prefix"},
      description = "The prefix skylb will apply to all metrics")
  private String metricsPrefix;

  @Parameter(names = {"--metrics-type", "-metrics-type"},
      description = "how skylb metrics will be reported, at least one of slf4j | jmx | http")
  private String metricsType;

  @Parameter(names = {"--metrics-slf4j-interval", "-metrics-slf4j-interval"},
      description = "the frequency metrics are emitted to the log, in seconds, when slf4j reporting is configured")
  private Duration metricsSlf4jInterval = Duration.ofSeconds(10);

  @Parameter(names = {"--http-port", "-http-port"},
      description = "the port the server will bind to when http reporting is configured")
  private int httpPort = 1920;

  @Parameter(names = {"--http-path-prefix", "-http-path-prefix"},
      description = "the http path prefix when metrics-type the http is enabled, default /")
  private String httpPathPrefix = "/";

  @Parameter(names = {"--http-bind-address", "-http-bind-address"},
      description = "the ip address the server will bind to when http reporting is configured")
  private InetAddress httpBindAddress = InetAddress.getLocalHost();

  @Parameter(names = {"--metrics-jvm", "-metrics-jvm"},
      description = "enable jvm metrics: true|false.")
  private boolean metricsJvm = false;

  public String producerType = "none";

  public MetricRegistry metricRegistry;
  public HealthCheckRegistry healthCheckRegistry;

  public MetricsConfig() throws UnknownHostException {
    this.metricRegistry = new MetricRegistry();
    this.healthCheckRegistry = new HealthCheckRegistry();
  }

  public String getMetricsPrefix() {
    return metricsPrefix;
  }

  public String getMetricsType() {
    return metricsType;
  }

  public Duration getMetricsSlf4jInterval() {
    return metricsSlf4jInterval;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public String getHttpPathPrefix() {
    return httpPathPrefix;
  }

  public InetAddress getHttpBindAddress() {
    return httpBindAddress;
  }

  public boolean isMetricsJvm() {
    return metricsJvm;
  }

  public void setProducerType(String producerType) {
    this.producerType = producerType;
  }

  @Override
  public String toKeyValues() {
    return new StringBuilder()
        .append("--metrics-prefix").append("=").append(this.getMetricsPrefix()).append("\n")
        .append("--metrics-type").append("=").append(this.getMetricsType()).append("\n")
        .append("--metrics-slf4j-interval").append("=").append(this.getMetricsSlf4jInterval())
        .append("\n")
        .append("--http-port").append("=").append(this.getHttpPort()).append("\n")
        .append("--http-path-prefix").append("=").append(this.getHttpPathPrefix()).append("\n")
        .append("--http-bind-address").append("=")
        .append(Objects.isNull(this.getHttpBindAddress()) ? "" : this.httpBindAddress.toString())
        .append("\n")
        .append("--metrics-jvm").append("=").append(this.isMetricsJvm()).append("\n")
        .toString();
  }
}
