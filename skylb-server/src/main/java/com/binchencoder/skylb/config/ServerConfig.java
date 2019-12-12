package com.binchencoder.skylb.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class ServerConfig {

  @Parameter(names = {"--port", "-port"},
      description = "The gRPC server port, e.g., 1900")
  private int port = 1900;

  @Parameter(names = {"--scrape-addr", "-scrape-addr"},
      description = "The address to listen on for HTTP requests., e.g., :1920")
  private String scrapeAddr = ":1920";

  @Parameter(names = {"--help", "-help", "--h", "-h"},
      description = "Print command line help", help = true)
  private boolean help = false;

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getScrapeAddr() {
    return scrapeAddr;
  }

  public void setScrapeAddr(String scrapeAddr) {
    this.scrapeAddr = scrapeAddr;
  }

  public boolean getHelp() {
    return help;
  }
}