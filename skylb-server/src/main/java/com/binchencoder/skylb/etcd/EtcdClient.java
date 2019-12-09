package com.binchencoder.skylb.etcd;

import static com.binchencoder.skylb.constants.EtcdConst.SEPARATOR;

import com.binchencoder.skylb.constants.EtcdConst;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.options.PutOption;
import java.util.Formatter;

public class EtcdClient {

  private final String etcdEndpoints;

  private final Client client;

  // The etcd key TTL.
  private static final int ETCD_KEY_TTL = 10;

  public EtcdClient(final String etcdEndpoints) {
    this.etcdEndpoints = etcdEndpoints;

    // create client
    this.client = Client.builder().endpoints(etcdEndpoints).build();
  }

  public final KV getKVClient() {
    if (null == client) {
      throw new NullPointerException("Etcd client initialization failed.");
    }

    return this.client.getKVClient();
  }

  public final Watch getWatchClient() {
    if (null == client) {
      throw new NullPointerException("Etcd client initialization failed.");
    }

    return this.client.getWatchClient();
  }

  private String calculateWeightKey(final String host, final int port) {
    return new Formatter().format("%s_%d_weight", host, port).toString();
  }

  public String calculateKey(final String namespace, final String serviceName) {
    return new StringBuilder(EtcdConst.ENDPOINTS_KEY)
        .append(SEPARATOR).append(namespace)
        .append(SEPARATOR).append(serviceName)
        .toString();
  }

  public String calculateEndpointKey(final String namespace, final String serviceName,
      final String host, final int port) {
    return new StringBuilder(EtcdConst.ENDPOINTS_KEY)
        .append(SEPARATOR).append(namespace)
        .append(SEPARATOR).append(serviceName)
        .append(SEPARATOR).append(new Formatter().format("%s_%d", host, port).toString())
        .toString();
  }

  public void refreshKey(final String key) {
    PutOption option = PutOption.newBuilder().build();

    this.getKVClient().put()
  }

  public void setKey(final String key, final ServiceSpec spec, final String host, final int port,
      final int weight) {

  }

}
