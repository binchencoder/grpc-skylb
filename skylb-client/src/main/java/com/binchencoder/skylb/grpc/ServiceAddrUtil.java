package com.binchencoder.skylb.grpc;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * ServiceAddrUtil 用来解析 skylb 和 direct 协议的混合字串。字串的格式如下:
 * skylb://a.b.c.d:1900,a.b.c.e:1900;direct://svcName:x.x.x.x:2016,svcName2:y.y.y.y:2017
 * 通过对字串进行解析，得到 skylb 的基地址，和一个服务名到直连地址的 map.
 */
public class ServiceAddrUtil {
  private static final IllegalArgumentException illegalArgumentException =
      new IllegalArgumentException("skylb address error");
  public static final String SKY_LB = "skylb";
  public static final String DIRECT = "direct"; // for direct connection.

  private String skylbAddr;
  private Map<String, String> directMap = new HashMap<String, String>();

  public String getSkylbAddr() {
    return this.skylbAddr;
  }

  public Map<String, String> getDirectMap() {
    return this.directMap;
  }

  public static ServiceAddrUtil parse(String confAddr) {
    if (confAddr == null || "".equals(confAddr.trim())) {
      throw illegalArgumentException;
    }
    String[] tmp = confAddr.split(";");
    if (tmp.length == 0) {
      throw illegalArgumentException;
    }

    int skylbSchemaCount = 0;
    int directSchemaCount = 0;
    int otherSchemaCount = 0;

    String directStr = "";
    ServiceAddrUtil result = new ServiceAddrUtil();

    for (String uri : tmp) {
      if (uri.startsWith(SKY_LB)) {
        if (uri.endsWith("/")) {
          uri = uri.substring(0, uri.length() - 1);
        }
        result.skylbAddr = uri;
        skylbSchemaCount++;
      } else if (uri.startsWith(DIRECT)) {
        directStr = uri.substring(DIRECT.length() + "://".length());
        directSchemaCount++;
      } else {
        otherSchemaCount++;
      }
    }

    if (skylbSchemaCount > 1 || directSchemaCount > 1 || otherSchemaCount != 0) {
      throw illegalArgumentException;
    }

    if (StringUtils.isBlank(directStr)) {
      return result;
    }

    String[] directs = directStr.split(",");
    for (String dir : directs) {
      String[] ss = dir.split(":");
      if (ss.length != 3) {
        throw illegalArgumentException;
      }
      String addrWithSchema = new StringBuilder(DIRECT).append("://")
          .append(dir.substring(ss[0].length() + 1)).toString();
      result.directMap.put(ss[0], addrWithSchema);
    }

    return result;
  }

}
