package com.binchencoder.skylb;

public class SkyLBConst {
  public static final String SKYLB_SCHEME = "skylb";
  public static final int SKYLB_PORT = 1900;

  public static final String ENDPOINT_OP = "op";
  public static final String ENDPOINT_WEIGHT = "weight";

  public static final String defaultNameSpace = "default";
  public static final String defaultPortName  = "port";

  public static final String URI_QUERY_PORTNAME = "portName";
  public static final String URI_QUERY_NAMESPACE = "namespace";

  /**
   * service names (string literal).
   * Also see comments for {@link com.binchencoder.skylb.demo.Configuration#CLIENT_SERVICE_NAME}
   */
  public static final String SKYLB_REPORTER = "skylb-reporter";
  public static final String SKYLB_DISCOVERY = "skylb-discovery";
  public static final String SKYLB_SERVER = "skylb-server";

  public static final String JG_ROUND_ROBIN = "jg_round-robin";
  public static final String JG_CONSISTEN_HASH = "jg-consisten-hash";
}
