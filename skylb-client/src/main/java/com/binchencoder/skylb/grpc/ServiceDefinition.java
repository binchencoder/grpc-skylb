package com.binchencoder.skylb.grpc;

import io.grpc.BindableService;

public class ServiceDefinition {
  BindableService service;
  String serviceName;

  public ServiceDefinition(BindableService service, String serviceName) {
    this.service = service;
    this.serviceName = serviceName;
  }

  public void setService(BindableService service) {
    this.service = service;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public BindableService getService() {
    return service;
  }

  public String getServiceName() {
    return serviceName;
  }
}
