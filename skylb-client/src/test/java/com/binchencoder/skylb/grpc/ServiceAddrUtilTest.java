package com.binchencoder.skylb.grpc;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class ServiceAddrUtilTest {
  @Test
  public void parseSkylbOne() throws Exception {
    ServiceAddrUtil res = ServiceAddrUtil.parse("skylb://1.2.3.4:1900");
    Assert.assertEquals("skylb://1.2.3.4:1900", res.getSkylbAddr());
    Assert.assertEquals(Collections.emptyMap(), res.getDirectMap());
  }

  @Test
  public void parseSkylbAndDirect() throws Exception {
    ServiceAddrUtil res = ServiceAddrUtil.parse("skylb://1.2.3.4:1900;direct://svc1:5.6.7.8:2016");
    Assert.assertEquals("skylb://1.2.3.4:1900", res.getSkylbAddr());
    Assert.assertEquals(1, res.getDirectMap().size());
    Assert.assertEquals("direct://5.6.7.8:2016", res.getDirectMap().get("svc1"));
  }

  @Test
  public void parseSkylbAndDirect2() throws Exception {
    ServiceAddrUtil res = ServiceAddrUtil.parse(
        "skylb://1.2.3.4:1900;direct://svc1:5.6.7.8:2016,svc2:9.10.11.12:2017");
    Assert.assertEquals("skylb://1.2.3.4:1900", res.getSkylbAddr());
    Assert.assertEquals(2, res.getDirectMap().size());
    Assert.assertEquals("direct://5.6.7.8:2016", res.getDirectMap().get("svc1"));
    Assert.assertEquals("direct://9.10.11.12:2017", res.getDirectMap().get("svc2"));
  }
}