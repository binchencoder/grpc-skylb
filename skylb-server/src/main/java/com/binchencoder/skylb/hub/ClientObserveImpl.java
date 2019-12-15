package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.hub.model.ClientObserver;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

/**
 * Implements interface ClientObserve.
 */
public class ClientObserveImpl implements ClientObserve {

  @Override
  public String clientAddr(ClientObserver observer) {
    return observer.getClientAddr();
  }

  @Override
  public ServiceSpec spec(ClientObserver observer) {
    return observer.getSpec();
  }

  @Override
  public void notify(ServiceEndpoints eps) {

  }

  @Override
  public void close(ClientObserver observer) {
    try {
      observer.getLock().lock();

      observer.setClosed(true);
    } finally {
      observer.getLock().unlock();
    }
  }
}
