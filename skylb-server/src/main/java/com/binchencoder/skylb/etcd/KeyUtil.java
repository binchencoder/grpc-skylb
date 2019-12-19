package com.binchencoder.skylb.etcd;

import static com.binchencoder.skylb.constants.EtcdConst.SEPARATOR;

import com.binchencoder.skylb.prefix.InitPrefix;
import java.util.Formatter;

public final class KeyUtil {

  /**
   * Returns the ETCD key for the given service.
   */
  public static String calculateWeightKey(final String host, final int port) {
    return new Formatter().format("%s_%d_weight", host, port).toString();
  }

  /**
   * Returns the ETCD key for the given endpoint.
   */
  public static String calculateKey(final String namespace, final String serviceName) {
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
        .append(SEPARATOR).append(new Formatter().format("%s_%d", host, port).toString())
        .toString();
  }
}
