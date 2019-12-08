package com.binchencoder.skylb.balancer.consistenthash;

import java.util.zip.CRC32;

public class Crc32Hash implements HashFunction {
  public int hash(String key) {
    CRC32 crc = new CRC32();
    crc.update(key.getBytes());
    return (int) crc.getValue();
  }
}
