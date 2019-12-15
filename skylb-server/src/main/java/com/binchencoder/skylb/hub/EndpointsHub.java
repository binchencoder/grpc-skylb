package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.common.GoChannelPool.GoChannel;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import java.net.SocketAddress;
import java.util.List;

/**
 * EndpointsHub defines the service endpoints hub based on etcd.
 */
public interface EndpointsHub {

  int ChanCapMultiplication = 10;

  /**
   * Adds an observer of the given service specs for the given clientAddr. When service endpoints
   * changed, it notifies the observer through the returned channel.
   */
  GoChannel<EndpointsUpdate> addObserver(List<ServiceSpec> specs, String clientAddr,
      Boolean resolveFull);

  /**
   * Removes the observer for the given service specs for the given clientAddr.
   */
  void removeObserver(List<ServiceSpec> specs, String clientAddr);

  /**
   * Inserts a service with the given namespace and service name.
   */
  void insertEndpoint(ServiceSpec spec, String host, String port, Integer weight);

  /**
   * Inserts or update a service with the given namespace and service name.
   */
  void upsertEndpoint(ServiceSpec spec, String host, String port, Integer weight);

  /**
   * Keeps track of dependency graph between clients and services.
   */
  void trackServiceGraph(ResolveRequest req, ServiceSpec callee, SocketAddress callerAddr);

  /**
   * Stops tracking of dependency graph between clients and services.
   */
  void untrackServiceGraph(ResolveRequest req, ServiceSpec callee, SocketAddress callerAddr);
}
