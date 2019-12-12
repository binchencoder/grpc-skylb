package com.binchencoder.skylb.interceptors;

import com.binchencoder.skylb.common.RpcContext;
import com.binchencoder.skylb.common.TraceContext;
import com.google.common.base.Stopwatch;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.opencensus.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderClientInterceptor implements ClientInterceptor {

  private static final Logger log = LoggerFactory.getLogger(HeaderClientInterceptor.class);
//  private final ZebraClientTracing clientTracing;

  public static ClientInterceptor instance() {
    return new HeaderClientInterceptor();
  }

  private HeaderClientInterceptor() {
//    clientTracing = SpringContextUtils.getBean(ZebraClientTracing.class);
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
      CallOptions callOptions, Channel next) {
    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
      // 判断API网关是否要打开调用链
//      boolean isGatewayTracing =
//          "1".equals(RpcContext.getContext().getAttachment(ZebraConstants.ZEBRA_OPEN_TRACING))
//              ? true : false;
      boolean isSubTracing =
          RpcContext.getContext().get(TraceContext.TRACE_ID_KEY) != null ? true : false;
      Stopwatch watch = null;
      Span span = null;

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
//        if (isSubTracing || isGatewayTracing) {
//          span = clientTracing.startTrace(method.getFullMethodName());
//          watch = Stopwatch.createStarted();
//        }
        copyThreadLocalToMetadata(headers);
        super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
          @Override
          public void onHeaders(Metadata headers) {
            super.onHeaders(headers);
          }

          @Override
          public void onClose(Status status, Metadata trailers) {
            super.onClose(status, trailers);
//            if (isSubTracing || isGatewayTracing) {
//              clientTracing.endTrace(span, watch, status.getCode().value());
//            }
          }
        }, headers);
      }
    };
  }

  private void copyThreadLocalToMetadata(Metadata headers) {
//    Map<String, String> attachments = RpcContext.getContext().getAttachments();
//    Map<String, Object> values = RpcContext.getContext().get();
//    try {
//      if (!attachments.isEmpty()) {
//        headers.put(GrpcUtil.GRPC_CONTEXT_ATTACHMENTS, SerializerUtil.toJson(attachments));
//      }
//      if (!values.isEmpty()) {
//        headers.put(GrpcUtil.GRPC_CONTEXT_VALUES, SerializerUtil.toJson(values));
//      }
//    } catch (Throwable e) {
//      log.error(e.getMessage(), e);
//    }
  }
}
