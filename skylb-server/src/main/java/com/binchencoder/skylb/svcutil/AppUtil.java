package com.binchencoder.skylb.svcutil;

import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppUtil.class);

  public static String getAppVersion() {
    try {
      ClassLoader classLoader = AppUtil.class.getClassLoader();
      if (classLoader != null) {
        Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
        while (resources.hasMoreElements()) {
          Manifest manifest = new Manifest(resources.nextElement().openStream());
          String ver = manifest.getMainAttributes().getValue("Implementation-Version");
          if (ver != null) {
            String title = manifest.getMainAttributes().getValue("Implementation-Title");
            if (null != title) {
              return title + "-" + ver;
            }

            return ver;
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Can't get manifest file", e);
    }

    return "UnImplementation";
  }
}
