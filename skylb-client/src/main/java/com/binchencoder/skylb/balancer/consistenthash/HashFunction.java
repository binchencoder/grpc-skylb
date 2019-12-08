package com.binchencoder.skylb.balancer.consistenthash;

public interface HashFunction {
  public int hash(String key);
}
