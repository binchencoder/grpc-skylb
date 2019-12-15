package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

public class ServiceObjectObserverImpl implements ServiceObjectObserver {

  @Override
  public ServiceSpec spec() {
    return null;
  }

  @Override
  public void addObserver() {

  }

  @Override
  public void removeObservers(String clientAddr) {

  }

  @Override
  public void setEndpoints(ServiceEndpoints endpoints) {

  }
}
