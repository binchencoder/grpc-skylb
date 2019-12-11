package com.binchencoder.skylb.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class ServerConfig {

  @Parameter(names = {"--grpc-port"},
      description = "The gRPC server port, e.g., 1900")
  private int port = 1900;

  @Parameter(names = {"--scrape-addr"},
      description = "The address to listen on for HTTP requests., e.g., :1920")
  private String scrapeAddr = ":1920";

  public int getPort() {
    return port;
  }

  public String getScrapeAddr() {
    return scrapeAddr;
  }
}
