package com.binchencoder.skylb.interceptors;

import com.binchencoder.skylb.trace.RpcContext;
import com.binchencoder.skylb.trace.TraceContext;
import com.binchencoder.skylb.trace.ZebraClientTracing;
import com.binchencoder.skylb.trace.constants.ZebraConst;
import com.binchencoder.skylb.trace.utils.GrpcUtil;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Span;

public class HeaderClientInterceptor implements ClientInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(HeaderClientInterceptor.class);
  private final ZebraClientTracing clientTracing;

  public static ClientInterceptor instance() {
    return new HeaderClientInterceptor();
  }

  private HeaderClientInterceptor() {
    clientTracing = ZebraClientTracing.getInstance();
  }

  @Override
  public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
      CallOptions callOptions, Channel next) {
    return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
      // 判断API网关是否要打开调用链
      boolean isGatewayTracing =
          "1".equals(RpcContext.getContext().getAttachment(ZebraConst.ZEBRA_OPEN_TRACING))
              ? true : false;
      boolean isSubTracing =
          RpcContext.getContext().get(TraceContext.TRACE_ID_KEY) != null ? true : false;
      Stopwatch watch = null;
      Span span = null;

      @Override
      public void start(Listener<RespT> responseListener, Metadata headers) {
        if (isSubTracing || isGatewayTracing) {
          span = clientTracing.startTrace(method.getFullMethodName());
          watch = Stopwatch.createStarted();
        }
        copyThreadLocalToMetadata(headers);
        super.start(new SimpleForwardingClientCallListener<RespT>(responseListener) {
          @Override
          public void onHeaders(Metadata headers) {
            super.onHeaders(headers);
          }

          @Override
          public void onClose(Status status, Metadata trailers) {
            super.onClose(status, trailers);
            if (isSubTracing || isGatewayTracing) {
              clientTracing.endTrace(span, watch, status.getCode().value());
            }
          }
        }, headers);
      }
    };
  }

  private void copyThreadLocalToMetadata(Metadata headers) {
    Map<String, String> attachments = RpcContext.getContext().getAttachments();
    Map<String, Object> values = RpcContext.getContext().get();
    try {
      if (!attachments.isEmpty()) {
        headers.put(GrpcUtil.GRPC_CONTEXT_ATTACHMENTS, new Gson().toJson(attachments));
      }
      if (!values.isEmpty()) {
        headers.put(GrpcUtil.GRPC_CONTEXT_VALUES, new Gson().toJson(values));
      }
    } catch (Throwable e) {
      LOGGER.error(e.getMessage(), e);
    }
  }
}
