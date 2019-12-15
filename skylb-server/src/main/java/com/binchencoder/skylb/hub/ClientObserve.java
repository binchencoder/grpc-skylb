package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.hub.model.ClientObserver;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

/**
 * ClientObserve defines the interface of a client observer for a gRPC service.
 */
public interface ClientObserve {

  /**
   * ClientAddr returns the client address of a client observer.
   */
  String clientAddr(ClientObserver observer);

  /**
   * Spec returns the spec of the service.
   */
  ServiceSpec spec(ClientObserver observer);

  /**
   * Notify notifies the client observer of the given service endpoints.
   */
  void notify(ServiceEndpoints eps);

  /**
   * Close closes the client observer.
   */
  void close(ClientObserver observer);
}
