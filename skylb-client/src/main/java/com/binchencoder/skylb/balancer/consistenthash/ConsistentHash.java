package com.binchencoder.skylb.balancer.consistenthash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import io.grpc.LoadBalancer.Subchannel;

/**
 * (Adapted from https://community.oracle.com/blogs/tomwhite/2007/11/27/consistent-hashing)
 */
public class ConsistentHash {

  static Logger logger = LoggerFactory.getLogger(ConsistentHash.class);

  private final HashFunction hashFunction;
  private final int numberOfReplicas;
  private final SortedMap<Integer, Subchannel> circle = new TreeMap<>();

  public ConsistentHash(HashFunction hashFunction, int numberOfReplicas,
                        Collection<Subchannel> nodes) {
    this.hashFunction = hashFunction;
    this.numberOfReplicas = numberOfReplicas;

    for (Subchannel node : nodes) {
      add(node);
    }
  }

  public void add(Subchannel node) {
    String target = getKey(node);
    synchronized (circle) {
      for (int i = 0; i < numberOfReplicas; i++) {
        circle.put(hashFunction.hash(i + target), node);
      }
    }
  }

  // Get key equal to golang version.
  private String getKey(Subchannel node) {
    String key = null;
    List<SocketAddress> addrs = node.getAddresses().getAddresses();
    if (addrs.size() > 0) {
      SocketAddress sa = addrs.get(0);
      if (sa instanceof InetSocketAddress) {
        InetSocketAddress addr = (InetSocketAddress) sa;
        // TODO(fuyc): may need to handle when address is hostname/domain
        // instead of IP.
        key = String.format("%s:%d", addr.getHostString(), addr.getPort());
      }
    }
    logger.debug("key {}", key);
    return key;
  }

  public void remove(Subchannel node) {
    String target = getKey(node);
    synchronized (circle) {
      for (int i = 0; i < numberOfReplicas; i++) {
        circle.remove(hashFunction.hash(i + target));
      }
    }
  }

  public Subchannel get(String key) {
    logger.debug("get {}", key);
    if (circle.isEmpty()) {
      return null;
    }
    int hash = hashFunction.hash(key);
    if (!circle.containsKey(hash)) {
      SortedMap<Integer, Subchannel> tailMap = circle.tailMap(hash);
      hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
    }
    return circle.get(hash);
  }
}