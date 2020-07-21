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
public final class Demo1Grpc {

  private Demo1Grpc() {}

  public static final String SERVICE_NAME = "proto.Demo1";

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
    if ((getGreetingMethod = Demo1Grpc.getGreetingMethod) == null) {
      synchronized (Demo1Grpc.class) {
        if ((getGreetingMethod = Demo1Grpc.getGreetingMethod) == null) {
          Demo1Grpc.getGreetingMethod = getGreetingMethod = 
              io.grpc.MethodDescriptor.<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest, com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "proto.Demo1", "Greeting"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new Demo1MethodDescriptorSupplier("Greeting"))
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
    if ((getGreetingForEverMethod = Demo1Grpc.getGreetingForEverMethod) == null) {
      synchronized (Demo1Grpc.class) {
        if ((getGreetingForEverMethod = Demo1Grpc.getGreetingForEverMethod) == null) {
          Demo1Grpc.getGreetingForEverMethod = getGreetingForEverMethod = 
              io.grpc.MethodDescriptor.<com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest, com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(
                  "proto.Demo1", "GreetingForEver"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new Demo1MethodDescriptorSupplier("GreetingForEver"))
                  .build();
          }
        }
     }
     return getGreetingForEverMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static Demo1Stub newStub(io.grpc.Channel channel) {
    return new Demo1Stub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static Demo1BlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new Demo1BlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static Demo1FutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new Demo1FutureStub(channel);
  }

  /**
   * <pre>
   * The gRPC service definition for SkyLB demo.
   * </pre>
   */
  public static abstract class Demo1ImplBase implements io.grpc.BindableService {

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
  public static final class Demo1Stub extends io.grpc.stub.AbstractStub<Demo1Stub> {
    private Demo1Stub(io.grpc.Channel channel) {
      super(channel);
    }

    private Demo1Stub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected Demo1Stub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new Demo1Stub(channel, callOptions);
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
  public static final class Demo1BlockingStub extends io.grpc.stub.AbstractStub<Demo1BlockingStub> {
    private Demo1BlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private Demo1BlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected Demo1BlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new Demo1BlockingStub(channel, callOptions);
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
  public static final class Demo1FutureStub extends io.grpc.stub.AbstractStub<Demo1FutureStub> {
    private Demo1FutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private Demo1FutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected Demo1FutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new Demo1FutureStub(channel, callOptions);
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
    private final Demo1ImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(Demo1ImplBase serviceImpl, int methodId) {
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

  private static abstract class Demo1BaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    Demo1BaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.binchencoder.skylb.demo.proto.GreetingProtos.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Demo1");
    }
  }

  private static final class Demo1FileDescriptorSupplier
      extends Demo1BaseDescriptorSupplier {
    Demo1FileDescriptorSupplier() {}
  }

  private static final class Demo1MethodDescriptorSupplier
      extends Demo1BaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    Demo1MethodDescriptorSupplier(String methodName) {
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
      synchronized (Demo1Grpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new Demo1FileDescriptorSupplier())
              .addMethod(getGreetingMethod())
              .addMethod(getGreetingForEverMethod())
              .build();
        }
      }
    }
    return result;
  }
}
