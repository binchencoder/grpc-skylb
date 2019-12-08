package com.binchencoder.skylb.util;

import com.binchencoder.grpc.data.DataProtos;

import com.binchencoder.grpc.data.DataProtos.ServiceId;
import org.junit.Assert;
import org.junit.Test;

public class ServiceNameUtilTest {
  @Test
  public void testToString() throws Exception {
    Assert.assertEquals("ease-gateway",
        ServiceNameUtil.toString(ServiceId.EASE_GATEWAY));
  }

  @Test
  public void toServiceId() throws Exception {
    Assert.assertEquals(ServiceId.EASE_GATEWAY,
        ServiceNameUtil.toServiceId("ease-gateway"));
  }
}