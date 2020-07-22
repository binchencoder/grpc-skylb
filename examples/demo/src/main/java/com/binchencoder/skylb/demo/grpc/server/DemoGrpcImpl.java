package com.binchencoder.skylb.demo.grpc.server;

import com.binchencoder.skylb.demo.proto.DemoGrpc;
import com.binchencoder.skylb.demo.proto.GreetingProtos;
import com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse;
import io.grpc.stub.StreamObserver;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoGrpcImpl extends DemoGrpc.DemoImplBase {

  private final Logger LOGGER = LoggerFactory.getLogger(DemoGrpcImpl.class);

  private Random rand = new Random(System.currentTimeMillis());

  private int port;

  public DemoGrpcImpl(int port_) {
    this.port = port_;
  }

  @Override
  public void greeting(GreetingProtos.GreetingRequest request,
      StreamObserver<GreetingProtos.GreetingResponse> responseObserver) {
    LOGGER.info("Got req: {}", request);

    // 随机耗时350~550毫秒.
    int elapse = 350 + rand.nextInt(200);
    try {
      TimeUnit.MILLISECONDS.sleep(elapse);
    } catch (InterruptedException ie) {
      LOGGER.warn("sleep interrupted");
    }

    GreetingResponse reply = GreetingResponse.newBuilder().setGreeting(
        "Hello " + request.getName() + ", from :" + port + ", elapse " + elapse + "ms").build();
    responseObserver.onNext(reply);
    responseObserver.onCompleted();
  }

  @Override
  public void greetingForEver(GreetingProtos.GreetingRequest request,
      StreamObserver<GreetingProtos.GreetingResponse> responseObserver) {
    super.greetingForEver(request, responseObserver);
  }
}
