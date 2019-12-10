package com.binchencoder.skylb.etcd;

import static com.binchencoder.skylb.constants.EtcdConst.SEPARATOR;

import com.binchencoder.skylb.constants.EtcdConst;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.PutOption;
import java.util.Formatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class EtcdClient {

  private final String etcdEndpoints;

  private final Client client;

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

  public final Lease getLeaseClient() {
    if (null == client) {
      throw new NullPointerException("Etcd client initialization failed.");
    }

    return this.client.getLeaseClient();
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

  public void refreshKey(final String key) throws ExecutionException, InterruptedException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Refresh the key should not be empty.");

    ByteSequence byteKey = ByteSequence.from(key.getBytes());
    // get the CompletableFuture
    CompletableFuture<GetResponse> getFuture = this.getKVClient().get(byteKey);
    // get the value from CompletableFuture
    GetResponse response = getFuture.get();

    for (KeyValue kv : response.getKvs()) {
    }

    PutOption option = PutOption.newBuilder().build();

  }

  public void setKey(final String key, final ServiceSpec spec, final String host, final int port,
      final int weight) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Set the key must be not empty.");
    // TODO(chenbin) Set etcd kv
  }

}
