package com.binchencoder.skylbweb.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.binchencoder.skylb.config.AbstractConfig;
import java.time.Duration;

@Parameters(separators = "=")
public class ServerConfig extends AbstractConfig {

  @Parameter(names = {"--within-k8s", "-within-k8s"},
      description = "Whether SkyLB is running in kubernetes")
  private boolean withInK8s = false;

  @Parameter(names = {"--http-port", "-http-port"},
      description = "the port the server will bind to when http reporting is configured")
  private int httpPort = 8081;

  @Parameter(names = {"--auto-rectify-interval", "-auto-rectify-interval"},
      description = "The interval of auto rectification. e.g. 10s(10 Seconds), 10m(10 Minutes)")
  private Duration autoRectifyInterval = Duration.ofSeconds(60);

  public boolean isWithInK8s() {
    return withInK8s;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public long getAutoRectifyInterval() {
    return autoRectifyInterval.toMillis();
  }

  @Override
  public String toKeyValues() {
    return new StringBuilder()
        .append("--http-port").append("=").append(this.getHttpPort()).append("\n")
        .append("--within-k8s").append("=").append(this.isWithInK8s()).append("\n")
        .append("--auto-rectify-interval").append("=").append(this.autoRectifyInterval.toString())
        .toString();
  }
}
