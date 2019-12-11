package com.binchencoder.skylb.etcd;

import static com.binchencoder.skylb.constants.EtcdConst.SEPARATOR;

import com.beust.jcommander.internal.Maps;
import com.binchencoder.skylb.config.EtcdConfig;
import com.binchencoder.skylb.constants.EtcdConst;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EtcdClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(EtcdClient.class);

  private final KV kvClient;
  private final Watch watchClient;
  private final Lease leaseClient;

  private final EtcdConfig etcdConfig;

  public EtcdClient(final EtcdConfig etcdConfig) {
    Preconditions.checkArgument(!Objects.isNull(etcdConfig), "EtcdConfig should be not null!");
    this.etcdConfig = etcdConfig;

    // create client
    Client client = Client.builder().endpoints(etcdConfig.getEndpoints()).build();
    if (null == client) {
      throw new NullPointerException("Etcd client initialization failed.");
    }

    this.kvClient = client.getKVClient();
    this.watchClient = client.getWatchClient();
    this.leaseClient = client.getLeaseClient();
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
    LOGGER.info("Refresh the key[{}] with ttl[{}]", key, etcdConfig.getEtcdKeyTtl());

    ByteSequence byteKey = ByteSequence.from(key.getBytes());
    // get the CompletableFuture
    CompletableFuture<GetResponse> getFuture = kvClient
        .get(byteKey, GetOption.newBuilder().withLimit(1).build());
    List<KeyValue> kvs = getFuture.get().getKvs();
    if (kvs.size() == 0) {
      throw EtcdExceptionFactory
          .newEtcdException(ErrorCode.DATA_LOSS, "Not found the kv with key:" + key);
    }

    for (KeyValue kv : kvs) {
      leaseClient.keepAliveOnce(kv.getLease());
    }
  }

  public void setKey(final String key, final ServiceSpec spec, final String host, final int port,
      final int weight) throws ExecutionException, InterruptedException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Set the key must be not empty.");

    Set<EndpointSubset> subsets = Sets.newHashSet(
        EndpointSubset.newBuilder()
            .setAddresses(Sets.newHashSet(EndpointAddress.newBuilder()
                .setIp(host)
                .setTargetRef(
                    ObjectReference.newBuilder().setNamespace(spec.getNamespace()).build())
                .build()))
            .setPorts(Sets.newHashSet(EndpointPort.newBuilder()
                .setName(spec.getNamespace())
                .setPort(port)
                .build()))
            .build()
    );

    Endpoints endpoints = Endpoints.newBuilder()
        .setSubSets(subsets)
        .setName(new Formatter().format("%s_%d", host, port).toString())
        .setNamespace(spec.getNamespace())
        .build();
    if (weight != 0) {
      endpoints
          .setLabels(Maps.newHashMap(calculateWeightKey(host, port),
              new Formatter().format("%d", weight).toString()));
    }

    long leaseID = leaseClient.grant(etcdConfig.getEtcdKeyTtl()).get().getID();
    String json = new Gson().toJson(endpoints);
    LOGGER.info("Etcd set key:{}, value:{} with leaseID[{}]", key, json, leaseID);
    kvClient.put(ByteSequence.from(key.getBytes()), ByteSequence.from(json.getBytes()),
        PutOption.newBuilder().withLeaseId(leaseID).build());
  }
}
