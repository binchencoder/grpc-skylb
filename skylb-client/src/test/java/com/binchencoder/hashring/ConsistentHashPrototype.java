package com.binchencoder.hashring;

import com.binchencoder.skylb.balancer.consistenthash.HashFunction;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * (Adapted from https://community.oracle.com/blogs/tomwhite/2007/11/27/consistent-hashing)
 *
 * <p>This is the origin of {@link com.binchencoder.skylb.balancer.consistenthash.ConsistentHash}
 * Keep the copy here for basic function testing.</p>
 */
public class ConsistentHashPrototype<T> {

  private final HashFunction hashFunction;
  private final int numberOfReplicas;
  private final SortedMap<Integer, T> circle = new TreeMap<Integer, T>();

  public ConsistentHashPrototype(HashFunction hashFunction, int numberOfReplicas,
                                 Collection<T> nodes) {
    this.hashFunction = hashFunction;
    this.numberOfReplicas = numberOfReplicas;

    for (T node : nodes) {
      add(node);
    }
  }

  public void add(T node) {
    synchronized (circle) {
      for (int i = 0; i < numberOfReplicas; i++) {
        circle.put(hashFunction.hash(i + node.toString()), node);
      }
    }
  }

  public void remove(T node) {
    synchronized (circle) {
      for (int i = 0; i < numberOfReplicas; i++) {
        circle.remove(hashFunction.hash(i + node.toString()));
      }
    }
  }

  public T get(String key) {
    if (circle.isEmpty()) {
      return null;
    }
    int hash = hashFunction.hash(key);
    if (!circle.containsKey(hash)) {
      SortedMap<Integer, T> tailMap = circle.tailMap(hash);
      hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
    }
    return circle.get(hash);
  }
}