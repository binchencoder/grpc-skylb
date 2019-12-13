package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

/**
 * ServiceObject defines the interface to manages service endpoints
 * and client observers for one service.
 */
public interface ServiceObjectObserver {

  /**
   * Spec returns the spec of the service.
   */
  ServiceSpec spec();

  /**
   * AddObserver adds a service client observer to the service object.
   */
  void addObserver();

  /**
   * RemoveObservers removes all client observers with the specified clientAddr
   * from the service object.
   */
  void removeObservers(String clientAddr);

  /**
   * SetEndpoints sets the service enpoints.
   */
  void setEndpoints(ServiceEndpoints endpoints);
}
