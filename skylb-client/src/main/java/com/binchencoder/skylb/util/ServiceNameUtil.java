package com.binchencoder.skylb.util;

import com.binchencoder.grpc.data.DataProtos.ServiceId;

public class ServiceNameUtil {

  private static final String UNDERLINE = "_";

  private static final String STRIKETHROUGH = "-";

  public static String toString(ServiceId serviceId) {
    return serviceId.name().toLowerCase().replace(UNDERLINE, STRIKETHROUGH);
  }

  public static ServiceId toServiceId(String serviceName) {
    return ServiceId.valueOf(serviceName.toUpperCase().replace(STRIKETHROUGH, UNDERLINE));
  }
}
