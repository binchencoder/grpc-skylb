package com.binchencoder.skylb.grpc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

/**
 * An interceptor to handle server header.
 */
public class HeaderServerInterceptor implements ServerInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(HeaderServerInterceptor.class);

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call,
      final Metadata requestHeaders,
      ServerCallHandler<ReqT, RespT> next) {
    logger.trace("header received from client:" + requestHeaders);

    String correlationId = requestHeaders.get(Constants.correlationIdHeadKey);
    if (StringUtils.isBlank(correlationId)) {
      correlationId = UUID.randomUUID().toString();
      MDC.put(Constants.CORRELATION_ID, correlationId);
      logger.info("Generated traceId:{}", correlationId);
    } else {
      MDC.put(Constants.CORRELATION_ID, correlationId);
      logger.info("{}:{}", Constants.metadataKeyTraceId, correlationId);
    }

    final String corrId = correlationId;
    return next.startCall(new SimpleForwardingServerCall<ReqT, RespT>(call) {
      @Override
      public void sendHeaders(Metadata responseHeaders) {
        responseHeaders.put(Constants.correlationIdHeadKey, corrId);
        MDC.put(Constants.CORRELATION_ID, corrId); // Required for logging.

        super.sendHeaders(responseHeaders);
      }

      @Override
      public void close(Status status, Metadata trailers) {
        super.close(status, trailers);
        MDC.remove(Constants.CORRELATION_ID);
      }
    }, requestHeaders);
  }
}
