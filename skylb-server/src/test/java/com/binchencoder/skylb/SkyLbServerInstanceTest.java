package com.binchencoder.skylb;

import com.binchencoder.cmd.skylb.SkyLbContext;
import org.junit.After;
import org.junit.Before;

public class SkyLbServerInstanceTest {

  protected SkyLbContext skyLbContext;

  @Before
  public void startup() throws Exception {
    skyLbContext = new SkyLbContext(
        new String[]{"--etcd-endpoints=http://192.168.47.16:2377", "--port=9876"});

    skyLbContext.start();
  }

  @After
  public void shutdown() throws Exception {
    if (skyLbContext != null) {
      skyLbContext.terminate();
    }
    //maybe need to clean the file store. But we do not suggest deleting anything.
  }

}
