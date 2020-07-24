package com.binchencoder.skylb.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

  static Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

  public static final String METRICS_IP = "0.0.0.0";
  public static final int METRICS_PORT = 1949;
  public static final String METRICS_PATH = "/_/metrics";

  // As han suggested, use string literals instead of enums defined in
  // gateway-proto/data, to void enforced dependency and
  // tight-coupling for the time being.
  public static final String CLIENT_SERVICE_NAME = "shared-test-client-service";
  public static final String SERVER_SERVICE_NAME = "shared-test-server-service";

  public static final int TEST_COUNT = 20000;
  public static final int DEADLINE_FOR_TEST = 700; // 毫秒
}