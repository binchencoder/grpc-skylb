package com.binchencoder.skylb.healthcheck;

import io.grpc.CallOptions.Key;

public class CallOption {
  public static final Key<String> HEALTH = Key.of("health", null);
}
