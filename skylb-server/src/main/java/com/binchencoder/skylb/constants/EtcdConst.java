package com.binchencoder.skylb.constants;

public class EtcdConst {

  public static final String SEPARATOR = "/";

  // EndpointsKey is the prefix for service endpoints key.
  public static final String ENDPOINTS_KEY = "/registry/services/endpoints";

  // GraphKey is the prefix for service graph key.
  public static final String GRAPH_KEY = "/skylb/graph";

  // LameduckKey is the prefix of the ETCD key for lameduck endpoints.
  public static final String LAMEDUCK_KEY = "/grpc/lameduck/services/";
}
