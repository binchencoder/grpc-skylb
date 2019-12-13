package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

/**
 * ClientObserver defines the interface of a client observer.
 */
public interface ClientObserver {

  /**
   * ClientAddr returns the client address of a client observer.
   */
  String clientAddr();

  /**
   * Spec returns the spec of the service.
   */
  ServiceSpec spec();

  /**
   * Notify notifies the client observer of the given service endpoints.
   */
  void notify(ServiceEndpoints eps);

  /**
   * Close closes the client observer.
   */
  void close();
}
