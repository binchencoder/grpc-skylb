package com.binchencoder.skylb.balancer.consistenthash;

import com.binchencoder.skylb.healthcheck.SizerLBFactory;
import com.binchencoder.skylb.healthcheck.SizerUser;

import io.grpc.CallOptions.Key;
import io.grpc.ExperimentalApi;
import io.grpc.LoadBalancer;
import io.grpc.NameResolver;

/**
 * A {@link LoadBalancer} that provides consistent hash load balancing mechanism
 * over the addresses from the {@link NameResolver}.
 *
 * This balancer is mostly a copy of the grpc RoundRobin balancer, with the
 * backing structure replaced with a ConsistentHash.
 */
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/1771")
public final class ConsistentHashLoadBalancerFactory extends LoadBalancer.Factory implements SizerLBFactory {

  private static ConsistentHashLoadBalancerFactory instance;

  private SizerUser sizerUser;

  // Declare and keep the key instance, which is used by load balancer to obtain value for hashing.
  // The values of name and default value are not important, and can be any string.
  public static final Key<String> HASHKEY = Key.of("conh", null);

  private ConsistentHashLoadBalancerFactory() {}

  /**
   * Gets the singleton instance of this factory.
   */
  public static synchronized ConsistentHashLoadBalancerFactory getInstance() {
    if (instance == null) {
      instance = new ConsistentHashLoadBalancerFactory();
    }
    return instance;
  }

  @Override
  public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
    ConsistentHashLoadBalancer lb = new ConsistentHashLoadBalancer(helper);
    if (null != this.sizerUser) {
      this.sizerUser.setSizer(lb);
    }
    return lb;
  }

  @Override
  public SizerLBFactory newInstance() {
    return new ConsistentHashLoadBalancerFactory();
  }

  @Override
  public void setSizerUser(SizerUser user) {
    this.sizerUser = user;
  }
}
