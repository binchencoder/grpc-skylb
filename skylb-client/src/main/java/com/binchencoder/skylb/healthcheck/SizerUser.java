package com.binchencoder.skylb.healthcheck;

public interface SizerUser {
  // Called back by lb factory.newLoadBalancer.
  void setSizer(Sizer sizer);
}
