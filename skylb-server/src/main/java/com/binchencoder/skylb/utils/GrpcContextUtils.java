package com.binchencoder.skylb.utils;

import com.binchencoder.skylb.interceptors.HeaderInterceptor;
import io.grpc.Context;
import java.net.SocketAddress;

/**
 * Utility functions for grpc context.
 */
public class GrpcContextUtils {

  /**
   * 获取访问用户的IP
   */
  public static String getRemoteIP() {
    return HeaderInterceptor.CONTEXT_VAL_ENTITY_KEY
        .get(Context.current()).remoteIP;
  }

  /**
   * 获取远程访问地址
   */
  public static SocketAddress getRemoteAddr() {
    return HeaderInterceptor.CONTEXT_VAL_ENTITY_KEY
        .get(Context.current()).remoteAddr;
  }

  public static class ContextEntity {

    // 访问用户IP
    private String remoteIP;

    // 远程访问地址
    private SocketAddress remoteAddr;

    public String getRemoteIP() {
      return remoteIP;
    }

    public void setRemoteIP(String remoteIP) {
      this.remoteIP = remoteIP;
    }

    public SocketAddress getRemoteAddr() {
      return remoteAddr;
    }

    public void setRemoteAddr(SocketAddress remoteAddr) {
      this.remoteAddr = remoteAddr;
    }

    public static Builder newBuilder() {
      return new Builder();
    }

    public static class Builder {

      private ContextEntity DEFAULT_INSTANCE;

      public Builder() {
        this.DEFAULT_INSTANCE = new ContextEntity();
      }

      public Builder setRemoteIP(String remoteIP) {
        this.DEFAULT_INSTANCE.setRemoteIP(remoteIP);
        return this;
      }

      public Builder setRemoteAddr(SocketAddress remoteAddr) {
        this.DEFAULT_INSTANCE.setRemoteAddr(remoteAddr);
        return this;
      }

      public ContextEntity build() {
        return this.DEFAULT_INSTANCE;
      }
    }
  }
}
