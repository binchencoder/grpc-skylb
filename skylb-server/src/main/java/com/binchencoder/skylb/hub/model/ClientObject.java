package com.binchencoder.skylb.hub.model;

import com.binchencoder.skylb.common.GoChannelPool.GoChannel;
import com.binchencoder.skylb.hub.EndpointsUpdate;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

public class ClientObject {

  private ServiceSpec serviceSpec;

  private String clientAddr;

  private Boolean resolveFull;

  private GoChannel<EndpointsUpdate> notifyChannel;

  public ServiceSpec getServiceSpec() {
    return serviceSpec;
  }

  public void setServiceSpec(ServiceSpec serviceSpec) {
    this.serviceSpec = serviceSpec;
  }

  public String getClientAddr() {
    return clientAddr;
  }

  public void setClientAddr(String clientAddr) {
    this.clientAddr = clientAddr;
  }

  public Boolean getResolveFull() {
    return resolveFull;
  }

  public void setResolveFull(Boolean resolveFull) {
    this.resolveFull = resolveFull;
  }

  public GoChannel<EndpointsUpdate> getNotifyChannel() {
    return notifyChannel;
  }

  public void setNotifyChannel(GoChannel<EndpointsUpdate> notifyChannel) {
    this.notifyChannel = notifyChannel;
  }
}
