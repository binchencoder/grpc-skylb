package com.binchencoder.skylb.healthcheck;

/**
 * The load balancer factory which can expose load balancer.
 */
public interface SizerLBFactory {
  /**
   * Create a new LB factory instance so we can store channels object.
   * This method is necessary, in case some client code still provides
   * XXLBFactory.getInstance() as initial LB factory object.
   *
   * TODO: Maybe not optimal for grpc usage.
   */
  SizerLBFactory newInstance();

  void setSizerUser(SizerUser user);
}
