package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import java.net.SocketAddress;

public interface SkyLbGraph extends AutoCloseable {

  /**
   * Keeps track of dependency graph between clients and services.
   */
  void trackServiceGraph(ResolveRequest req, ServiceSpec callee, SocketAddress callerAddr);

  void trackServiceGraph(ResolveRequest req, SocketAddress callerAddr);

  /**
   * Stops tracking of dependency graph between clients and services.
   */
  void untrackServiceGraph(ResolveRequest req, SocketAddress callerAddr);
}
