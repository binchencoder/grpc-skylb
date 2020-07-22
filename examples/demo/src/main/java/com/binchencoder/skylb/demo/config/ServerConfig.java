package com.binchencoder.skylb.demo.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class ServerConfig extends AbstractConfig {

  @Parameter(names = {"--skylb-address", "-skylb-address"},
      description = "The address of skylb server.")
  private String skylbAddr = "skylb://127.0.0.1:1900/";

  @Parameter(names = {"--port", "-port"},
      description = "The gRPC server port, e.g., 9090")
  private int port = 9090;

  @Parameter(names = {"--port-name", "-port-name"},
      description = "The gRPC server port name, e.g., grpc")
  private String portName = "grpc";

  @Parameter(names = {"--within-k8s", "-within-k8s"},
      description = "Whether SkyLB is running in kubernetes")
  private boolean withInK8s = false;

  public String getSkylbAddr() {
    return skylbAddr;
  }

  public int getPort() {
    return port;
  }

  public String getPortName() {
    return portName;
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
        .append("--skylb-address").append("=").append(this.getSkylbAddr()).append("\n")
        .append("--port").append("=").append(this.getPort()).append("\n")
        .append("--port-name").append("=").append(this.getPortName()).append("\n")
        .append("--within-k8s").append("=").append(this.isWithInK8s())
        .toString();
  }
}
