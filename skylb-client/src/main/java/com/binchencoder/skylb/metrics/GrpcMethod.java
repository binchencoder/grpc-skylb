package com.binchencoder.skylb.metrics;

import io.grpc.MethodDescriptor;
import io.grpc.MethodDescriptor.MethodType;

class GrpcMethod {
  private final String serviceName; // = callee service
  private final String methodName;
  private final MethodType type;
  private final String callerServiceName;

  static GrpcMethod of(MethodDescriptor<?, ?> method, String callerServiceName, String calleeServiceName) {
    String serviceName = MethodDescriptor.extractFullServiceName(method.getFullMethodName());
    String methodName = method.getFullMethodName().substring(serviceName.length() + 1);
    serviceName = calleeServiceName; // replace with callee service id/name
    return new GrpcMethod(serviceName, methodName, method.getType(), callerServiceName);
  }

  private GrpcMethod(String serviceName, String methodName, MethodType type, String callerServiceName) {
    this.serviceName = serviceName;
    this.methodName = methodName;
    this.type = type;
    this.callerServiceName = callerServiceName;
  }

  String serviceName() {
    return this.serviceName;
  }

  String methodName() {
    return this.methodName;
  }

  String type() {
    return this.type.toString();
  }

  String callerServiceName() {
    return this.callerServiceName;
  }

  boolean streamsRequests() {
    return this.type == MethodType.CLIENT_STREAMING
        || this.type == MethodType.BIDI_STREAMING;
  }

  boolean streamsResponses() {
    return this.type == MethodType.SERVER_STREAMING
        || this.type == MethodType.BIDI_STREAMING;
  }
}
