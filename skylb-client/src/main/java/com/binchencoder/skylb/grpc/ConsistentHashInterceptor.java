package com.binchencoder.skylb.grpc;

import com.binchencoder.skylb.balancer.consistenthash.ConsistentHashLoadBalancerFactory;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A interceptor to set HashKey.
 */
public class ConsistentHashInterceptor implements ClientInterceptor {

  private static final Logger logger = LoggerFactory.getLogger(ConsistentHashInterceptor.class);

  // Key of hash key in GRPC implementation.
  private static final String metadataKeyHashKey = "mdkey_hashkey";

  private static final Metadata.Key<String> correlationIdHashKey =
      Metadata.Key.of(metadataKeyHashKey, Metadata.ASCII_STRING_MARSHALLER);

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
      final CallOptions callOptions, Channel next) {
    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        String hashKey = callOptions.getOption(ConsistentHashLoadBalancerFactory.HASHKEY);

        logger.debug("hashKey: {}", hashKey);
        if (null != hashKey) {
          // Health checking used to have null hashkey.
          headers.put(correlationIdHashKey, hashKey);
        }
        super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
          @Override
          public void onHeaders(Metadata headers) {
            /**
             * if you don't need receive header from server, you can use
             * {@link io.grpc.stub.MetadataUtils attachHeaders} directly to send header
             */
            logger.debug("header received from server: {}", headers);
            super.onHeaders(headers);
          }
        }, headers);
      }
    };
  }
}
