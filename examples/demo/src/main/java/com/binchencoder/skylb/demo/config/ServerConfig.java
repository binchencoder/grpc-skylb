package com.binchencoder.skylb.demo.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class ServerConfig extends AbstractConfig {

  @Parameter(names = {"--port", "-port"},
      description = "The gRPC server port, e.g., 1900")
  private int port = 1900;

  @Parameter(names = {"--within-k8s", "-within-k8s"},
      description = "Whether SkyLB is running in kubernetes")
  private boolean withInK8s = false;

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public boolean isWithInK8s() {
    return withInK8s;
  }

  @Override
  public String toKeyValues() {
    return new StringBuilder()
        .append("--port").append("=").append(this.getPort()).append("\n")
        .append("--within-k8s").append("=").append(this.isWithInK8s())
        .toString();
  }
}
