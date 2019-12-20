package com.binchencoder.skylb.etcd;

import static com.binchencoder.skylb.constants.EtcdConst.SEPARATOR;

import com.binchencoder.skylb.prefix.InitPrefix;

public final class KeyUtil {

  /**
   * Returns the ETCD key for the given service.
   */
  public static String calculateWeightKey(final String host, final int port) {
    return String.format("%s_%d_weight", host, port);
  }

  /**
   * Returns the ETCD key for the given endpoint.
   */
  public static String calculateEndpointKey(final String namespace, final String serviceName) {
    return new StringBuilder(InitPrefix.ENDPOINTS_KEY)
        .append(SEPARATOR).append(namespace)
        .append(SEPARATOR).append(serviceName)
        .toString();
  }

  /**
   * Returns the ETCD key for the given endpoint.
   */
  public static String calculateEndpointKey(final String namespace, final String serviceName,
      final String host, final int port) {
    return new StringBuilder(InitPrefix.ENDPOINTS_KEY)
        .append(SEPARATOR).append(namespace)
        .append(SEPARATOR).append(serviceName)
        .append(SEPARATOR).append(String.format("%s_%d", host, port))
        .toString();
  }

  /**
   * Returns the ETCD key for the given graph endpoint.
   */
  public static String calculateClientGraphKey(final String namespace, final String serviceName,
      final String callerServiceName) {
    return new StringBuilder(InitPrefix.GRAPH_KEY)
        .append(SEPARATOR).append(namespace)
        .append(SEPARATOR).append(serviceName)
        .append(SEPARATOR).append("clients")
        .append(SEPARATOR).append(callerServiceName)
        .toString();
  }
}
