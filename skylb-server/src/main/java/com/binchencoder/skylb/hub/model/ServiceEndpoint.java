package com.binchencoder.skylb.hub.model;

import java.util.Formatter;

/**
 * ServiceEndpoint represents a service endpoint.
 * (A simplified version of pb.InstanceEndpoint)
 */
public class ServiceEndpoint {

  private String ip;

  private Integer port;

  private Integer weight;

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public Integer getWeight() {
    return weight;
  }

  public void setWeight(Integer weight) {
    this.weight = weight;
  }

  @Override
  public String toString() {
    return new Formatter().format("%s:%d", ip, port).toString();
  }
}
