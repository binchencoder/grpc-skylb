package com.binchencoder.skylb.prefix;

import com.binchencoder.skylb.etcd.EtcdClient;
import com.google.protobuf.ByteString;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitPrefix {

  private static final Logger LOGGER = LoggerFactory.getLogger(InitPrefix.class);

  // EndpointsKey is the prefix for service endpoints key.
  public static final String ENDPOINTS_KEY = "/registry/services/endpoints";

  // GraphKey is the prefix for service graph key.
  public static final String GRAPH_KEY = "/skylb/graph";

  // LameduckKey is the prefix of the ETCD key for lameduck endpoints.
  public static final String LAMEDUCK_KEY = "/grpc/lameduck/services/";

  private static EtcdClient etcdClient = null;
  private static InitPrefix ONECE_INSTANCE = null;

  private InitPrefix(EtcdClient etcdClient) {
    this.etcdClient = etcdClient;
  }

  public static void newInstance(final EtcdClient etcdClient) {
    if (null == ONECE_INSTANCE) {
      ONECE_INSTANCE = new InitPrefix(etcdClient);

      // Initializes ETCD keys.
      mustExist(ENDPOINTS_KEY);
      mustExist(GRAPH_KEY);
      mustExist(LAMEDUCK_KEY);
    } else {
      LOGGER.warn("InitPrefix can only be initialized once.");
    }
  }

  private static void mustExist(String key) {
    KV kvClient = etcdClient.getKvClient();

    ByteSequence byteKey = ByteSequence.from(key.getBytes());
    GetResponse resp;
    boolean preExist = false;
    try {
      resp = kvClient.get(byteKey, GetOption.newBuilder().withCountOnly(true).build()).get();
      if (resp.getCount() > 0) {
        preExist = true;
      }
    } catch (InterruptedException | ExecutionException e) {
      LOGGER.error("Etcd client get key error, key[{}]", key);
      return;
    }

    // For whatever reason it failed, let's try to create the key.
    if (!preExist) {
      try {
        boolean isDone = kvClient
            .put(byteKey, ByteSequence.from(ByteString.EMPTY), PutOption.newBuilder().build())
            .isDone();
      } catch (Exception e) {
        throw e;
      }
    }
  }
}
