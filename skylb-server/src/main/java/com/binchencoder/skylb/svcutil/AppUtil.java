package com.binchencoder.skylb.svcutil;

import java.io.IOException;
import java.util.Properties;

public class AppUtil {

  private static String appVersion;

  public static String getAppVersion() {
    if (null == appVersion) {
      Properties properties = new Properties();
      try {
        properties.load(AppUtil.class.getClassLoader().getResourceAsStream("application.properties"));
        if (!properties.isEmpty()) {
          appVersion = properties.getProperty("version");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return appVersion;
  }

}
