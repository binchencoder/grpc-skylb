package com.binchencoder.skylb.lameduck;

import com.binchencoder.skylb.etcd.EtcdClient;
import com.google.common.collect.Lists;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.watch.WatchEvent;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Pattern;

public class LameDuck {

  // KeyPattern is the regexp pattern of the ETCD key for lameduck endpoints.
  private static final Pattern KEY_PATTERN = Pattern
      .compile("^/grpc/lameduck/services/([^/]*)/endpoints/([^/]*)$");

  private final List<String> lameducks = Lists.newArrayListWithExpectedSize(100);

  private final EtcdClient etcdClient;

  public LameDuck(EtcdClient etcdClient) {
    this.etcdClient = etcdClient;
  }

  /**
   * HostPort combines the given host and port into a lameduck endpoint.
   */
  public String formatHostPort(String host, String port) {
    return new Formatter().format("%s#%s", host, port).toString();
  }

  /**
   * IsLameduckMode returns true if the given endpoint is in lameduck mode.
   */
  public boolean isLameduckMode(String endpoint) {
    return lameducks.contains(endpoint);
  }

  /**
   * Recursively extracts the lameduck endpoints from the given ETCD nodes.
   * @param response
   */
  public void extractLameduck(GetResponse response) {
    // TODO(chenbin) implement it
  }

  /**
   * Extracts the lameduck endpoints and operation from watch response of ETCD.
   */
  public void extractLameduckChange(List<WatchEvent> events) {
    // TODO(chenbin) implement it
  }
}
