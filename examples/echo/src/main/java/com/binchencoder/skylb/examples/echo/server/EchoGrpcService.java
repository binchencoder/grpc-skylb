package com.binchencoder.skylb.examples.echo.server;

import com.binchencoder.easegw.examples.EchoServiceGrpc;
import com.binchencoder.easegw.examples.ExamplesProto.SimpleMessage;
import com.binchencoder.grpc.GrpcService;
import com.binchencoder.grpc.errors.Errors.ErrorCode;
import com.binchencoder.grpc.exceptions.BadRequestException;
import com.binchencoder.grpc.interceptors.ExceptionInterceptor;
import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService(applyGlobalInterceptors = false, interceptors = ExceptionInterceptor.class)
public class EchoGrpcService extends EchoServiceGrpc.EchoServiceImplBase {

  private final Logger LOGGER = LoggerFactory.getLogger(EchoGrpcService.class);

  @Override
  public void echo(SimpleMessage request, StreamObserver<SimpleMessage> responseObserver) {
    responseObserver.onNext(request.toBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void echoBody(SimpleMessage request, StreamObserver<SimpleMessage> responseObserver) {
    LOGGER.info("echoBody: {}", request.toString());
    responseObserver.onNext(request.toBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void echoDelete(SimpleMessage request, StreamObserver<SimpleMessage> responseObserver) {
    BadRequestException ex = new BadRequestException(ErrorCode.BAD_REQUEST,
        "Bad request echo delete");
    ex.setParams(Lists.newArrayList("1", "2"));
    throw ex;
  }
}
