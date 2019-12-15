package com.binchencoder.skylb.hub.model;

import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServiceObject {

  private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private ServiceSpec serviceSpec;

  private Map<String, ServiceEndpoint> endpoints;

  private List<ClientObject> observers;

  public ReentrantReadWriteLock getReadWriteLock() {
    return readWriteLock;
  }

  public ServiceSpec getServiceSpec() {
    return serviceSpec;
  }

  public void setServiceSpec(ServiceSpec serviceSpec) {
    this.serviceSpec = serviceSpec;
  }

  public Map<String, ServiceEndpoint> getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(
      Map<String, ServiceEndpoint> endpoints) {
    this.endpoints = endpoints;
  }

  public List<ClientObject> getObservers() {
    if (null == observers) {
      this.observers = new ArrayList<>();
    }
    return observers;
  }

  public void setObservers(List<ClientObject> observers) {
    this.observers = observers;
  }
}
