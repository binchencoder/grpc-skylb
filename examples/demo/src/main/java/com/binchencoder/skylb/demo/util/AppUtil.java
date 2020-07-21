package com.binchencoder.skylb.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppUtil.class);

  public static String getVersion() {
    String pkgVersion = AppUtil.class.getPackage().getImplementationVersion();
    if (pkgVersion == null) {
      return "??";
    } else {
      String pkgTitle = AppUtil.class.getPackage().getImplementationTitle();
      if (null != pkgTitle) {
        return pkgTitle + "-" + pkgVersion;
      }

      return pkgVersion;
    }
  }
}
