package com.binchencoder.skylb.etcd;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Maps;
import com.binchencoder.skylb.etcd.Endpoints.EndpointPort;
import com.binchencoder.skylb.etcd.Endpoints.EndpointSubset;
import com.binchencoder.skylb.etcd.Endpoints.EndpointSubset.EndpointAddress;
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
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import java.util.Formatter;
import java.util.List;
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

  public static EtcdConfig etcdConfig = new EtcdConfig();

  public EtcdClient() {
//    Preconditions.checkArgument(!Objects.isNull(etcdConfig), "EtcdConfig should be not null!");

    LOGGER.info("Initializing the etcd client, etcd-endpoints: {}", etcdConfig.getEndpoints());
    // create client
    Client client = Client.builder().endpoints(etcdConfig.getEndpoints()).build();
    Preconditions.checkNotNull(client, "Failed to initialized etcd client.");

    // TODO(chenbin) 检查client是否连接成功

    this.kvClient = client.getKVClient();
    Preconditions.checkNotNull(kvClient, "Failed to initialized kv client.");
    this.watchClient = client.getWatchClient();
    Preconditions.checkNotNull(watchClient, "Failed to initialized watch client.");
    this.leaseClient = client.getLeaseClient();
    Preconditions.checkNotNull(leaseClient, "Failed to initialized lease client.");
  }

  public void refreshKey(final String key) throws ExecutionException, InterruptedException {
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(key), "Refresh the key should not be empty.");
    LOGGER.info("Refresh the key[{}] with ttl[{}]s", key, etcdConfig.getEtcdKeyTtl());

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

  public CompletableFuture<PutResponse> setKey(final String key, final ServiceSpec spec,
      final String host, final int port, final int weight)
      throws ExecutionException, InterruptedException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "Set the key must be not empty.");

    Set<EndpointSubset> subsets = Sets.newHashSet(
        EndpointSubset.newBuilder()
            .setAddresses(Sets.newHashSet(EndpointAddress.newBuilder()
                .setIp(host)
                .setTargetRef(
                    ObjectReference.newBuilder().setNamespace(spec.getNamespace()).build())
                .build()))
            .setPorts(Sets.newHashSet(EndpointPort.newBuilder()
                .setName(spec.getPortName())
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
          .setLabels(Maps.newHashMap(KeyUtil.calculateWeightKey(host, port),
              new Formatter().format("%d", weight).toString()));
    }

    long leaseID = leaseClient.grant(etcdConfig.getEtcdKeyTtl()).get().getID();
    String json = new Gson().toJson(endpoints);
    LOGGER.info("Etcd set key:{}, value:{} with leaseID[{}]", key, json, leaseID);
    return kvClient.put(ByteSequence.from(key.getBytes()), ByteSequence.from(json.getBytes()),
        PutOption.newBuilder().withLeaseId(leaseID).build());
  }

  public KV getKvClient() {
    return kvClient;
  }

  public Watch getWatchClient() {
    return watchClient;
  }

  public Lease getLeaseClient() {
    return leaseClient;
  }

  @Parameters(separators = "=", commandDescription = "Print etcd options")
  public static class EtcdConfig {

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
}
