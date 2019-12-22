package com.binchencoder.skylb.etcd;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import io.etcd.jetcd.common.exception.EtcdException;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import com.binchencoder.skylb.SkyLbServerInstanceTest;
import com.binchencoder.skylb.TestUtil;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link EtcdClient}. */
@RunWith(JUnit4.class)
public class EtcdClientTest extends SkyLbServerInstanceTest {

  protected EtcdClient etcdClient;

  @Before
  public void before() {
    etcdClient = skyLbContext.getEtcdClient();
  }

  @Test(expected = EtcdException.class)
  public void refreshKeyNotExist() throws ExecutionException, InterruptedException {
    final String key = TestUtil.randomString();
    etcdClient.refreshKey(key);
  }

  @Test
  public void setKey() throws ExecutionException, InterruptedException {
    final String key = TestUtil.randomString();
    ServiceSpec spec = ServiceSpec.newBuilder()
        .setServiceName("skylb-test")
        .setNamespace("default")
        .setPortName("port")
        .build();
    etcdClient.setEndpointsKey(key, spec, "127.0.0.1", 1212, 0);
  }

  @Test
  public void setAndRefreshKey() throws ExecutionException, InterruptedException {
    final String key = TestUtil.randomString();
    ServiceSpec spec = ServiceSpec.newBuilder()
        .setServiceName("skylb-test-service")
        .setNamespace("default")
        .setPortName("port")
        .build();
    PutResponse putResp = etcdClient.setEndpointsKey(key, spec, "127.0.0.1", 1213, 0).get();
    Assert.assertNull(putResp.getPrevKv());

    // Get the put key
    GetResponse getResp = etcdClient.getKvClient().get(TestUtil.bytesOf(key)).get();
    Assert.assertEquals(key, getResp.getKvs().get(0).getKey().toString(UTF_8));

    etcdClient.refreshKey(key);
  }
}
