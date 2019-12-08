package com.binchencoder.skylb.grpc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

/**
 * A interceptor to handle client header.
 */
public class HeaderClientInterceptor implements ClientInterceptor {
  private static final Logger logger = LoggerFactory.getLogger(HeaderClientInterceptor.class);

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                             CallOptions callOptions, Channel next) {
    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        String correlationId = MDC.get(Constants.CORRELATION_ID);
        if (StringUtils.isBlank(correlationId)) {
          correlationId = UUID.randomUUID().toString();
          MDC.put(Constants.CORRELATION_ID, correlationId);
          logger.info("Generated traceId:{}", correlationId);
        } else {
          logger.info("{}:{}", Constants.metadataKeyTraceId, correlationId);
        }

        /* put X-Request-Id header */
        headers.put(Constants.correlationIdHeadKey, correlationId);
        super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
          @Override
          public void onHeaders(Metadata headers) {
            /**
             * if you don't need receive header from server, you can use
             * {@link io.grpc.stub.MetadataUtils attachHeaders} directly to send header
             */
            logger.trace("header received from server: {}", headers);
            super.onHeaders(headers);
          }
        }, headers);
      }

      @Override
      public void halfClose() {
        super.halfClose();
        MDC.remove(Constants.CORRELATION_ID);
      }
    };
  }
}
