package com.binchencoder.skylb.trace.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;

public class NetUtil {

  /**
   * 获取本机网卡IP地址，规则如下：
   *
   * <pre>
   * 1. 查找所有网卡地址，必须非回路（loopback）地址、非局域网地址（siteLocal）、IPv4地址
   * 2. 如果无满足要求的地址，调用 {@link InetAddress#getLocalHost()} 获取地址
   * </pre>
   *
   * 此方法不会抛出异常，获取失败将返回<code>null</code><br>
   *
   * 见：https://github.com/looly/hutool/issues/428
   *
   * @return 本机网卡IP地址，获取失败返回<code>null</code>
   * @since 3.0.1
   */
  public static InetAddress getLocalhost() {
    final LinkedHashSet<InetAddress> localAddressList = localAddressList(address -> {
      // 非loopback地址，指127.*.*.*的地址
      return false == address.isLoopbackAddress()
          // 非地区本地地址，指10.0.0.0 ~ 10.255.255.255、172.16.0.0 ~ 172.31.255.255、192.168.0.0 ~ 192.168.255.255
          && false == address.isSiteLocalAddress()
          // 需为IPV4地址
          && address instanceof Inet4Address;
    });

    if (null != localAddressList && !localAddressList.isEmpty()) {
      return get(localAddressList, 0);
    }

    try {
      return InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      // ignore
    }

    return null;
  }

  /**
   * 获取所有满足过滤条件的本地IP地址对象
   *
   * @param addressFilter 过滤器，null表示不过滤，获取所有地址
   * @return 过滤后的地址对象列表
   * @since 4.5.17
   */
  public static LinkedHashSet<InetAddress> localAddressList(Filter<InetAddress> addressFilter) {
    Enumeration<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      throw new UtilException(e);
    }

    if (networkInterfaces == null) {
      throw new UtilException("Get network interface error!");
    }

    final LinkedHashSet<InetAddress> ipSet = new LinkedHashSet<>();

    while (networkInterfaces.hasMoreElements()) {
      final NetworkInterface networkInterface = networkInterfaces.nextElement();
      final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        final InetAddress inetAddress = inetAddresses.nextElement();
        if (inetAddress != null && (null == addressFilter || addressFilter.accept(inetAddress))) {
          ipSet.add(inetAddress);
        }
      }
    }

    return ipSet;
  }

  /**
   * 获取集合中指定下标的元素值，下标可以为负数，例如-1表示最后一个元素<br>
   * 如果元素越界，返回null
   *
   * @param <T>        元素类型
   * @param collection 集合
   * @param index      下标，支持负数
   * @return 元素值
   * @since 4.0.6
   */
  private static <T> T get(Collection<T> collection, int index) {
    if (null == collection) {
      return null;
    }

    final int size = collection.size();
    if (0 == size) {
      return null;
    }

    if (index < 0) {
      index += size;
    }

    // 检查越界
    if (index >= size) {
      return null;
    }

    if (collection instanceof List) {
      final List<T> list = ((List<T>) collection);
      return list.get(index);
    } else {
      int i = 0;
      for (T t : collection) {
        if (i > index) {
          break;
        } else if (i == index) {
          return t;
        }
        i++;
      }
    }
    return null;
  }
}
