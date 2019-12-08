package com.binchencoder.skylb.grpchealth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * A {@link ServerInterceptor} which prints client address.
 */
public class JinHealthServiceInterceptor implements ServerInterceptor {
  static Logger logger = LoggerFactory.getLogger(JinHealthServiceInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> serverCall,
      Metadata metadata,
      ServerCallHandler<ReqT, RespT> serverCallHandler) {
    logger.debug("Health check from client {}", serverCall.getAttributes()
        .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR));
    return serverCallHandler.startCall(serverCall, metadata);
  }
}
