package com.binchencoder.skylb.grpchealth;

import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;

/**
 * A simple grpc health service implementation, similar to "echo".
 *
 * (The stock io.grpc.services.HealthServiceImp mandates "service"
 * as parameter, which is a bit heavy to use)
 */
public class JinHealthServiceImpl extends HealthGrpc.HealthImplBase {

  @Override
  public void check(HealthCheckRequest request,
                    StreamObserver<HealthCheckResponse> responseObserver) {
    HealthCheckResponse response = HealthCheckResponse.newBuilder()
        .setStatus(HealthCheckResponse.ServingStatus.SERVING)
        .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
