package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;

public class EndpointsUpdate {

  private long id;

  private ServiceEndpoints endpoints;

  public EndpointsUpdate(long id, ServiceEndpoints endpoints) {
    this.id = id;
    this.endpoints = endpoints;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public ServiceEndpoints getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(ServiceEndpoints endpoints) {
    this.endpoints = endpoints;
  }
}
