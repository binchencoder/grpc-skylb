package com.binchencoder.skylb;

import com.google.common.base.Strings;

import com.binchencoder.skylb.proto.ClientProtos.InstanceEndpoint;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkyLBUtils {
  private static final Logger logger = LoggerFactory.getLogger(SkyLBUtils.class);

  private static final String ServiceSpecPrefix = "ServiceSpec:";
  private static final String ServerInfoPrefix = "ServerInfos:";

  /**
   * parseAddress parses authority, returns a list of InetSocketAddress.
   */
  public static List<InetSocketAddress> parseAddress(String authority, int defaultPort) {
    String[] ipports = authority.split(",");
    List<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>(ipports.length);
    for (String ipport : ipports) {
      if (Strings.isNullOrEmpty(ipport)) {
        continue;
      }
      String hostStr = "";
      int port = defaultPort;

      String[] temp = ipport.split(":");
      if (temp.length > 0) {
        hostStr = temp[0];
      }
      if (temp.length > 1) {
        port = Integer.parseInt(temp[1]);
      }

      addrs.add(new InetSocketAddress(hostStr, port));
    }

    return addrs;
  }

  /**
   * endpointsToString return a string for a list of endpoint.
   */
  public static String endpointsToString(ServiceEndpoints edp) {
    StringBuilder sb = new StringBuilder(ServiceSpecPrefix);
    sb.append(edp.getSpec().getServiceName()).append("-")
        .append(edp.getSpec().getNamespace()).append("-").append(edp.getSpec().getPortName())
        .append("\n[");
    for (InstanceEndpoint ie : edp.getInstEndpointsList()) {
      sb.append("h:").append(ie.getHost()).append(",p:").append(ie.getPort()).append(",o:")
          .append(ie.getOp().toString()).append(",w:").append(ie.getWeight()).append(";");
    }
    sb.append("]\n");

    return sb.toString();
  }

  /**
   * serverInfoToString returns a string for a list of list of
   * ResolvedServerInfo.
   *
   * Applies for grpc java 1.2.0+.
   */
  public static String serverInfoToString(List<ResolvedServerInfoGroup> servers) {
    StringBuilder sb = new StringBuilder(ServerInfoPrefix)
        .append(servers.size()).append("\n[");
    for (ResolvedServerInfoGroup list : servers) {
      for (ResolvedServerInfo info : list.getResolvedServerInfoList()) {
        sb.append(info.toString()).append(" ");
      }
      sb.append("\n");
    }
    sb.append("]\n");
    return sb.toString();
  }

  public static ServiceSpec buildServiceSpec(String namespace, String serviceName,
                                             String portName) {

    namespace = namespace.trim();
    serviceName = serviceName.trim();
    portName = portName.trim();
    if (StringUtils.isEmpty(serviceName)) {
      return null;
    }
    namespace = StringUtils.isEmpty(namespace) ? SkyLBConst.defaultNameSpace : namespace;
    portName = StringUtils.isEmpty(portName) ? SkyLBConst.defaultPortName : portName;

    return ServiceSpec.newBuilder()
        .setNamespace(namespace)
        .setServiceName(serviceName)
        .setPortName(portName).build();
  }

  public static Map<String, String> parseQuery(URI uri) {
    Map<String, String> queryPairs = new LinkedHashMap<String, String>();
    String query = uri.getQuery();
    if (StringUtils.isBlank(query)) {
      return queryPairs;
    }

    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      if (idx == -1) {
        continue;
      }
      try {
        queryPairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.toString()),
            URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.toString()));
      } catch (UnsupportedEncodingException e) {
        logger.warn(e.getMessage());
      }
    }
    return queryPairs;
  }

  public static void checkServiceName(String name) {
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (Character.isLowerCase(ch) || '-' == ch || Character.isDigit(ch)) {
        continue;
      }
      System.err.println("Service name " + name + " contains invalid character: " + ch);
      System.exit(2);
    }
  }
}
