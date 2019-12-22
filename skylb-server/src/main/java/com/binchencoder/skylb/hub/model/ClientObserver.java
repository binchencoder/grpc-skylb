package com.binchencoder.skylb.hub.model;

import com.binchencoder.common.GoChannelQueue;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientObserver {

  private static final Lock lock = new ReentrantLock(true);

  private ServiceSpec spec;

  private String clientAddr;

  private boolean closed;

  private GoChannelQueue<ServiceEndpoints> notifyCh;

  public Lock getLock() {
    return lock;
  }

  public ServiceSpec getSpec() {
    return spec;
  }

  public void setSpec(ServiceSpec spec) {
    this.spec = spec;
  }

  public String getClientAddr() {
    return clientAddr;
  }

  public void setClientAddr(String clientAddr) {
    this.clientAddr = clientAddr;
  }

  public boolean isClosed() {
    return closed;
  }

  public void setClosed(boolean closed) {
    this.closed = closed;
  }

  public GoChannelQueue<ServiceEndpoints> getNotifyCh() {
    return notifyCh;
  }

  public void setNotifyCh(
      GoChannelQueue<ServiceEndpoints> notifyCh) {
    this.notifyCh = notifyCh;
  }
}
