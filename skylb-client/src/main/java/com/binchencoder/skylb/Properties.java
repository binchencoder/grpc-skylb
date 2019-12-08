package com.binchencoder.skylb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System property names for flags.
 */
public class Properties {
  public static final String WITHIN_K8S = "within-k8s";
  public static final String withinK8s = System.getProperty(Properties.WITHIN_K8S, "false");

  public static final String DEBUG_SVC_INTERVAL = "debug-svc-interval";
  static long debugSvcInterval = Long.parseLong(System.getProperty(DEBUG_SVC_INTERVAL, "60"));

  public static final String ENABLE_HEALTH_CHECK = "enable-health-check";
  public static boolean enableHealthCheck = Boolean.parseBoolean(System.getProperty(ENABLE_HEALTH_CHECK, "true"));

  public static final String HEALTH_CHECK_INTERVAL = "health-check-interval";
  public static long healthCheckIntervalInSec = Long.parseLong(System.getProperty(HEALTH_CHECK_INTERVAL, "60"));

  public static final String HEALTH_CHECK_TIMEOUT = "health-check-timeout";
  public static long healthCheckTimeoutInMillis = Long.parseLong(System.getProperty(HEALTH_CHECK_TIMEOUT, "2000"));

  public static final String GRPC_IDLE_TIMEOUT = "grpc-idle-timeout";
  /**
   * For testing purpose.
   * Set -Dgrpc-idle-timeout=value (>0) to reproduce grpc idle timeout behavior.
   */
  public static final long grpcIdleTimeoutInSec = Long.parseLong(System.getProperty(GRPC_IDLE_TIMEOUT, "0"));

  public static final String MAX_INBOUND_MESSAGE_SIZE = "max-inbound-message-size";
 /**
  * Set -Dmax-inbound-message-size=value (>0) to set maxInboundMessageSize of channel.
  */
  public static final int maxInboundMessageSize = Integer.parseInt(System.getProperty(MAX_INBOUND_MESSAGE_SIZE, "4194304"));

  static Logger logger = LoggerFactory.getLogger(Properties.class);

  static {
    logger.debug("{}", System.getProperties());
  }
}
