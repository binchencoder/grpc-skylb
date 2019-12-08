package com.binchencoder.skylb;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

public class Spec implements Comparable<Spec> {
  private String serviceName;
  private String namespace;
  private String portName;

  public Spec(String serviceName, String namespace, String portName) {
    this.serviceName = serviceName;
    this.namespace = namespace;
    this.portName = portName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getPortName() {
    return portName;
  }

  public void setPortName(String portName) {
    this.portName = portName;
  }

  @Override
  public int compareTo(Spec o) {
    int result = this.namespace.compareTo(o.namespace);
    if (result != 0) {
      return result;
    }

    result = this.serviceName.compareTo(o.serviceName);
    if (result != 0) {
      return result;
    }

    return this.portName.compareTo(o.portName);
  }

  @Override
  public String toString() {
    return this.toStringHelper().toString();
  }

  protected ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this)
        .add("servcieName", this.getServiceName())
        .add("namespace", this.getNamespace())
        .add("portName", this.getPortName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getNamespace(), this.getPortName(), this.getServiceName());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    Spec other = (Spec) obj;
    return Objects.equals(this.namespace, other.namespace)
        && Objects.equals(this.portName, other.portName)
        && Objects.equals(this.serviceName, other.serviceName);
  }

  public ServiceSpec toServiceSpec() {
    return ServiceSpec.newBuilder()
        .setNamespace(this.getNamespace())
        .setServiceName(this.getServiceName())
        .setPortName(this.getPortName()).build();
  }
}
