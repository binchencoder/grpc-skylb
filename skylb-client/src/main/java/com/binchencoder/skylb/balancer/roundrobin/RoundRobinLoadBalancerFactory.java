package com.binchencoder.skylb.balancer.roundrobin;

import com.binchencoder.skylb.healthcheck.SizerLBFactory;
import com.binchencoder.skylb.healthcheck.SizerUser;

import io.grpc.ExperimentalApi;
import io.grpc.LoadBalancer;

/**
 * A {@link LoadBalancer} that provides round-robin load balancing mechanism over the
 * addresses.
 */
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/1771")
public final class RoundRobinLoadBalancerFactory extends LoadBalancer.Factory implements SizerLBFactory {

  private static RoundRobinLoadBalancerFactory instance;
  private SizerUser sizerUser;

  private RoundRobinLoadBalancerFactory() {}

  /**
   * Gets the singleton instance of this factory.
   */
  public static synchronized RoundRobinLoadBalancerFactory getInstance() {
    if (instance == null) {
      instance = new RoundRobinLoadBalancerFactory();
    }
    return instance;
  }

  @Override
  public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
    RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer(helper);
    if (null != this.sizerUser) {
      this.sizerUser.setSizer(lb);
    }
    return lb;
  }

  @Override
  public SizerLBFactory newInstance() {
    return new RoundRobinLoadBalancerFactory();
  }

  @Override
  public void setSizerUser(SizerUser user) {
    this.sizerUser = user;
  }
}
