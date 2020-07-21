package com.binchencoder.skylb.demo.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 * <pre>
 * The gRPC service definition for SkyLB demo.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.18.0)",
    comments = "Source: api.proto")
public final class DemoGrpc {

  private DemoGrpc() {}

  public static final String SERVICE_NAME = "proto.Demo";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest,
      com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> getGreetingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Greeting",
      requestType = com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest.class,
      responseType = com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest,
      com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> getGreetingMethod() {
    io.grpc.MethodDescriptor<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest, com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> getGreetingMethod;
    if ((getGreetingMethod = DemoGrpc.getGreetingMethod) == null) {
      synchronized (DemoGrpc.class) {
        if ((getGreetingMethod = DemoGrpc.getGreetingMethod) == null) {
          DemoGrpc.getGreetingMethod = getGreetingMethod = 
              io.grpc.MethodDescriptor.<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest, com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Demo", "Greeting"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DemoMethodDescriptorSupplier("Greeting"))
                  .build();
          }
        }
     }
     return getGreetingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest,
      com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> getGreetingForEverMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GreetingForEver",
      requestType = com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest.class,
      responseType = com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest,
      com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> getGreetingForEverMethod() {
    io.grpc.MethodDescriptor<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest, com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> getGreetingForEverMethod;
    if ((getGreetingForEverMethod = DemoGrpc.getGreetingForEverMethod) == null) {
      synchronized (DemoGrpc.class) {
        if ((getGreetingForEverMethod = DemoGrpc.getGreetingForEverMethod) == null) {
          DemoGrpc.getGreetingForEverMethod = getGreetingForEverMethod = 
              io.grpc.MethodDescriptor.<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest, com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(
                  "proto.Demo", "GreetingForEver"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DemoMethodDescriptorSupplier("GreetingForEver"))
                  .build();
          }
        }
     }
     return getGreetingForEverMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DemoStub newStub(io.grpc.Channel channel) {
    return new DemoStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DemoBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new DemoBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DemoFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new DemoFutureStub(channel);
  }

  /**
   * <pre>
   * The gRPC service definition for SkyLB demo.
   * </pre>
   */
  public static abstract class DemoImplBase implements io.grpc.BindableService {

    /**
     */
    public void greeting(com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest request,
        io.grpc.stub.StreamObserver<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGreetingMethod(), responseObserver);
    }

    /**
     */
    public void greetingForEver(com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest request,
        io.grpc.stub.StreamObserver<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGreetingForEverMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getGreetingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest,
                com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse>(
                  this, METHODID_GREETING)))
          .addMethod(
            getGreetingForEverMethod(),
            asyncServerStreamingCall(
              new MethodHandlers<
                com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest,
                com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse>(
                  this, METHODID_GREETING_FOR_EVER)))
          .build();
    }
  }

  /**
   * <pre>
   * The gRPC service definition for SkyLB demo.
   * </pre>
   */
  public static final class DemoStub extends io.grpc.stub.AbstractStub<DemoStub> {
    private DemoStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DemoStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DemoStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DemoStub(channel, callOptions);
    }

    /**
     */
    public void greeting(com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest request,
        io.grpc.stub.StreamObserver<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGreetingMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void greetingForEver(com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest request,
        io.grpc.stub.StreamObserver<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getGreetingForEverMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * The gRPC service definition for SkyLB demo.
   * </pre>
   */
  public static final class DemoBlockingStub extends io.grpc.stub.AbstractStub<DemoBlockingStub> {
    private DemoBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DemoBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DemoBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DemoBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse greeting(com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest request) {
      return blockingUnaryCall(
          getChannel(), getGreetingMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> greetingForEver(
        com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest request) {
      return blockingServerStreamingCall(
          getChannel(), getGreetingForEverMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * The gRPC service definition for SkyLB demo.
   * </pre>
   */
  public static final class DemoFutureStub extends io.grpc.stub.AbstractStub<DemoFutureStub> {
    private DemoFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DemoFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DemoFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new DemoFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse> greeting(
        com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGreetingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GREETING = 0;
  private static final int METHODID_GREETING_FOR_EVER = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DemoImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(DemoImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GREETING:
          serviceImpl.greeting((com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest) request,
              (io.grpc.stub.StreamObserver<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse>) responseObserver);
          break;
        case METHODID_GREETING_FOR_EVER:
          serviceImpl.greetingForEver((com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest) request,
              (io.grpc.stub.StreamObserver<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class DemoBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DemoBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.binchencoder.skylb.demo.proto.GreetingProtos.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Demo");
    }
  }

  private static final class DemoFileDescriptorSupplier
      extends DemoBaseDescriptorSupplier {
    DemoFileDescriptorSupplier() {}
  }

  private static final class DemoMethodDescriptorSupplier
      extends DemoBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DemoMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (DemoGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new DemoFileDescriptorSupplier())
              .addMethod(getGreetingMethod())
              .addMethod(getGreetingForEverMethod())
              .build();
        }
      }
    }
    return result;
  }
}
