package com.binchencoder.skylb;

import static org.junit.Assert.assertTrue;

import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.EtcdClient.EtcdConfig;
import org.junit.After;
import org.junit.Before;

public class SkyLbServerInstanceTest {

  protected SkyLbController skyLbController;
  protected EtcdConfig etcdConfig = new EtcdConfig();
  protected ServerConfig serverConfig = new ServerConfig();

  @Before
  public void startup() throws Exception {
    serverConfig.setPort(9876);
    etcdConfig.setEndpoints("http://192.168.47.16:2377");

    skyLbController = new SkyLbController(serverConfig);
    boolean initResult = skyLbController.initialize();
    assertTrue(initResult);

    skyLbController.start();
  }

  @After
  public void shutdown() throws Exception {
    if (skyLbController != null) {
      skyLbController.shutdown();
    }
    //maybe need to clean the file store. But we do not suggest deleting anything.
  }

}
