package com.binchencoder.skylb.interceptors;

import com.binchencoder.skylb.trace.RpcContext;
import com.binchencoder.skylb.trace.TraceContext;
import com.binchencoder.skylb.trace.ZebraServerTracing;
import com.binchencoder.skylb.trace.constants.ZebraConst;
import com.binchencoder.skylb.trace.utils.GrpcUtil;
import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.grpc.ForwardingServerCall.SimpleForwardingServerCall;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import java.net.InetSocketAddress;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.Span;

public class HeaderServerInterceptor implements ServerInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(HeaderServerInterceptor.class);

  private final ZebraServerTracing serverTracing;

  public static ServerInterceptor instance() {
    return new HeaderServerInterceptor();
  }

  private HeaderServerInterceptor() {
    serverTracing = ZebraServerTracing.getInstance();
  }

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
      final Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    return next.startCall(new SimpleForwardingServerCall<ReqT, RespT>(call) {
      boolean isSubTracing =
          RpcContext.getContext().get(TraceContext.TRACE_ID_KEY) != null ? true : false;
      Stopwatch watch = null;
      Span span = null;

      @Override
      public void request(int numMessages) {
        if (isSubTracing) {
          span = serverTracing.startTrace(call.getMethodDescriptor().getFullMethodName());
          watch = Stopwatch.createStarted();
        }
        InetSocketAddress remoteAddress = (InetSocketAddress) call.getAttributes()
            .get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        RpcContext.getContext()
            .setAttachment(ZebraConst.REMOTE_ADDRESS, remoteAddress.getHostString());
        copyMetadataToThreadLocal(headers);
        LOGGER.debug("FullMethodName:{}, RemoteAddress={}, attachments={}, context={}",
            call.getMethodDescriptor().getFullMethodName(), remoteAddress.getHostString(),
            headers.get(GrpcUtil.GRPC_CONTEXT_ATTACHMENTS),
            headers.get(GrpcUtil.GRPC_CONTEXT_VALUES));
        super.request(numMessages);
      }

      @Override
      public void close(Status status, Metadata trailers) {
        delegate().close(status, trailers);
        if (isSubTracing) {
          serverTracing.endTrace(span, watch, status.getCode().value());
        }
      }

    }, headers);
  }

  private void copyMetadataToThreadLocal(Metadata headers) {
    String attachments = headers.get(GrpcUtil.GRPC_CONTEXT_ATTACHMENTS);
    String values = headers.get(GrpcUtil.GRPC_CONTEXT_VALUES);
    try {
      if (attachments != null) {
        Map<String, String> attachmentsMap = new Gson().fromJson(attachments,
            new TypeToken<Map<String, String>>() {
            }.getType());
        RpcContext.getContext().setAttachments(attachmentsMap);
      }

      if (values != null) {
        Map<String, Object> valuesMap = new Gson().fromJson(values,
            new TypeToken<Map<String, Object>>() {
            }.getType());
        for (Map.Entry<String, Object> entry : valuesMap.entrySet()) {
          RpcContext.getContext().set(entry.getKey(), entry.getValue());
        }
      }
    } catch (Throwable e) {
      LOGGER.error(e.getMessage(), e);
    }
  }
}
