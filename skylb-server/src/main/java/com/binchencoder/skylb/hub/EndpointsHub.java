package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import java.net.InetAddress;
import java.util.List;

public interface EndpointsHub {

  /**
   * Adds an observer of the given service specs for the given clientAddr. When service endpoints
   * changed, it notifies the observer through the returned channel.
   */
  public EndpointsUpdate addObserver(List<ServiceSpec> specs, String clientAddr,
      Boolean resolveFull);

  /**
   * Removes the observer for the given service specs for the given clientAddr.
   */
  public void removeObserver(List<ServiceSpec> specs, String clientAddr);

  /**
   * Inserts a service with the given namespace and service name.
   */
  public void insertEndpoint(ServiceSpec spec, String host, String port, Integer weight);

  /**
   * Inserts or update a service with the given namespace and service name.
   */
  public void UpsertEndpoint(ServiceSpec spec, String host, String port, Integer weight);

  /**
   * Keeps track of dependency graph between clients and services.
   */
  public void trackServiceGraph(ResolveRequest req, ServiceSpec callee, InetAddress callerAddr);

  /**
   * Stops tracking of dependency graph between clients and services.
   */
  public void untrackServiceGraph(ResolveRequest req, ServiceSpec callee, InetAddress callerAddr);
}
