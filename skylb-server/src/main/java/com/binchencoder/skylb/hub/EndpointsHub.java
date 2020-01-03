package com.binchencoder.skylb.hub;

import com.binchencoder.common.GoChannelQueue;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import java.io.Closeable;
import java.util.List;

/**
 * EndpointsHub defines the service endpoints hub based on etcd.
 */
public interface EndpointsHub extends AutoCloseable {

  int ChanCapMultiplication = 10;

  /**
   * Adds an observer of the given service specs for the given clientAddr. When service endpoints
   * changed, it notifies the observer through the returned channel.
   */
  GoChannelQueue<EndpointsUpdate> addObserver(List<ServiceSpec> specs, String clientAddr,
      Boolean resolveFull) throws InterruptedException;

  /**
   * Removes the observer for the given service specs for the given clientAddr.
   */
  void removeObserver(List<ServiceSpec> specs, String clientAddr);

  /**
   * Inserts a service with the given namespace and service name.
   */
  void insertEndpoint(ServiceSpec spec, String host, int port, Integer weight);

  /**
   * Inserts or update a service with the given namespace and service name.
   */
  void upsertEndpoint(ServiceSpec spec, String host, int port, Integer weight);

  void updateEndpoints(String key);
}
