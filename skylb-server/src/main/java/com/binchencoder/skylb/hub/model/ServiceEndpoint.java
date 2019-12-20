package com.binchencoder.skylb.hub.model;

/**
 * ServiceEndpoint represents a service endpoint. (A simplified version of pb.InstanceEndpoint)
 */
public class ServiceEndpoint {

  private String ip;

  private int port;

  private int weight;

  public ServiceEndpoint() {
  }

  public ServiceEndpoint(String ip, int port) {
    this.ip = ip;
    this.port = port;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  @Override
  public String toString() {
    return String.format("%s:%d", ip, port);
  }
}
