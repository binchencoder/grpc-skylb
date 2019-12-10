package com.binchencoder.skylb.hub;

import com.binchencoder.skylb.hub.model.ClientObject;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import java.net.InetAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointsHubImpl implements EndpointsHub {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointsHubImpl.class);

  @Override
  public EndpointsUpdate addObserver(List<ServiceSpec> specs, String clientAddr,
      Boolean resolveFull) {
    if (null == specs || specs.isEmpty()) {
      return null;
    }

    for (ServiceSpec spec : specs) {
      LOGGER.info("Resolve service {}.{} on port name {} from client {}", spec.getNamespace(),
          spec.getServiceName(), spec.getPortName(), clientAddr);

      ClientObject co = new ClientObject();
      co.setServiceSpec(spec);
      co.setClientAddr(clientAddr);
      co.setResolveFull(resolveFull);
    }

    return null;
  }

  @Override
  public void removeObserver(List<ServiceSpec> specs, String clientAddr) {

  }

  @Override
  public void insertEndpoint(ServiceSpec spec, String host, String port, Integer weight) {

  }

  @Override
  public void upsertEndpoint(ServiceSpec spec, String host, String port, Integer weight) {

  }

  @Override
  public void trackServiceGraph(ResolveRequest req, ServiceSpec callee, InetAddress callerAddr) {

  }

  @Override
  public void untrackServiceGraph(ResolveRequest req, ServiceSpec callee, InetAddress callerAddr) {

  }
}
