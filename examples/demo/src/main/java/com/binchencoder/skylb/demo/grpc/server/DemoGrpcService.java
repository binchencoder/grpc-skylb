package com.binchencoder.skylb.demo.grpc.server;

import com.binchencoder.skylb.demo.proto.DemoGrpc;
import com.binchencoder.skylb.demo.proto.GreetingProtos;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoGrpcService extends DemoGrpc.DemoImplBase {

  private final Logger LOGGER = LoggerFactory.getLogger(DemoGrpcService.class);

  @Override
  public void greeting(GreetingProtos.GreetingRequest request,
      StreamObserver<GreetingProtos.GreetingResponse> responseObserver) {
    super.greeting(request, responseObserver);
  }

  @Override
  public void greetingForEver(GreetingProtos.GreetingRequest request,
      StreamObserver<GreetingProtos.GreetingResponse> responseObserver) {
    super.greetingForEver(request, responseObserver);
  }
}
