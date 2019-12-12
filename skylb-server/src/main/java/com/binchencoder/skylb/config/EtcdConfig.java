package com.binchencoder.skylb.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class EtcdConfig {

  @Parameter(names = {"--etcd-endpoints", "-etcd-endpoints"},
      description = "The comma separated ETCD endpoints, e.g., http://etcd1:2379,http://etcd2:2379")
  private String endpoints = "http://127.0.0.1:2379";

  @Parameter(names = {"--etcd-key-ttl", "-etcd-key-ttl"},
      description = "The etcd key time-to-live in seconds")
  private int etcdKeyTtl = 10;

  public String getEndpoints() {
    return endpoints;
  }

  public void setEndpoints(String endpoints) {
    this.endpoints = endpoints;
  }

  public int getEtcdKeyTtl() {
    return etcdKeyTtl;
  }

  public void setEtcdKeyTtl(int etcdKeyTtl) {
    this.etcdKeyTtl = etcdKeyTtl;
  }
}