package com.binchencoder.skylb.etcd;

import java.util.Set;

/**
 * Endpoints is a collection of endpoints that implement the actual service.
 *
 * Example:
 * <pre>
 *   Name: "mysvc",
 *   Subsets: [
 *     {
 *       Addresses: [{"ip": "10.10.1.1"}, {"ip": "10.10.2.2"}],
 *       Ports: [{"name": "a", "port": 8675}, {"name": "b", "port": 309}]
 *     },
 *     {
 *       Addresses: [{"ip": "10.10.3.3"}],
 *       Ports: [{"name": "a", "port": 93}, {"name": "b", "port": 76}]
 *     },
 *  ]
 *  </pre>
 */
public class Endpoints {

  /**
   * The set of all endpoints is the union of all subsets. Addresses are placed into subsets
   * according to the IPs they share. A single address with multiple ports, some of which are ready
   * and some of which are not (because they come from different containers) will result in the
   * address being displayed in different subsets for the different ports. No address will appear in
   * both Addresses and NotReadyAddresses in the same subset.
   *
   * Sets of addresses and ports that comprise a service.
   */
  private Set<EndpointSubset> subsets;

  public Set<EndpointSubset> getSubsets() {
    return subsets;
  }

  public void setSubsets(Set<EndpointSubset> subsets) {
    this.subsets = subsets;
  }
}

/**
 * EndpointSubset is a group of addresses with a common set of ports. The expanded set of endpoints
 * is the Cartesian product of Addresses x Ports.
 *
 * <pre>
 * For example, given:
 *   {
 *     Addresses: [{"ip": "10.10.1.1"}, {"ip": "10.10.2.2"}],
 *     Ports:     [{"name": "a", "port": 8675}, {"name": "b", "port": 309}]
 *   }
 *
 * The resulting set of endpoints can be viewed as:
 *     a: [ 10.10.1.1:8675, 10.10.2.2:8675 ],
 *     b: [ 10.10.1.1:309, 10.10.2.2:309 ]
 *
 * </pre>
 */
class EndpointSubset {

  /**
   * IP addresses which offer the related ports that are marked as ready. These endpoints should be
   * considered safe for load balancers and clients to utilize.
   */
  private Set<EndpointAddress> addresses;

  /**
   * IP addresses which offer the related ports but are not currently marked as ready because they
   * have not yet finished starting, have recently failed a readiness check, or have recently failed
   * a liveness check.
   */
  private Set<EndpointAddress> notReadyAddresses;

  /**
   * Port numbers available on the related IP addresses.
   */
  private Set<EndpointPort> ports;

  public Set<EndpointAddress> getAddresses() {
    return addresses;
  }

  public void setAddresses(Set<EndpointAddress> addresses) {
    this.addresses = addresses;
  }

  public Set<EndpointAddress> getNotReadyAddresses() {
    return notReadyAddresses;
  }

  public void setNotReadyAddresses(
      Set<EndpointAddress> notReadyAddresses) {
    this.notReadyAddresses = notReadyAddresses;
  }

  public Set<EndpointPort> getPorts() {
    return ports;
  }

  public void setPorts(Set<EndpointPort> ports) {
    this.ports = ports;
  }
}

/**
 * EndpointAddress is a tuple that describes single IP address.
 */
class EndpointAddress {

  /**
   * The IP of this endpoint.
   *
   * May not be loopback (127.0.0.0/8), link-local (169.254.0.0/16), or link-local multicast
   * ((224.0.0.0/24).
   *
   * IPv6 is also accepted but not fully supported on all platforms. Also, certain kubernetes
   * components, like kube-proxy, are not IPv6 ready.
   */
  private String ip;

  /**
   * The Hostname of this endpoint.
   */
  private String hostname;

  /**
   * Node hosting this endpoint. This can be used to determine endpoints local to a node.
   */
  private String nodeName;

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getNodeName() {
    return nodeName;
  }

  public void setNodeName(String nodeName) {
    this.nodeName = nodeName;
  }
}

/**
 * a tuple that describes a single port.
 */
class EndpointPort {

  /**
   * The name of this port (corresponds to ServicePort.Name).
   *
   * Must be a DNS_LABEL. Optional only if one port is defined.
   */
  private String name;

  /**
   * The port number of the endpoint.
   */
  private String port;

  /**
   * The IP protocol for this port.
   *
   * Must be UDP or TCP. Default is TCP.
   */
  private String protocol;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }
}

/**
 * ObjectReference contains enough information to let you inspect or modify the referred object.
 */
class ObjectReference {

  private static final String DEFAULT_KIND = "Pod";

  /**
   * Kind of the referent.
   */
  private String kind = DEFAULT_KIND;

  /**
   * Namespace of the referent.
   */
  private String namespace;

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }
}
