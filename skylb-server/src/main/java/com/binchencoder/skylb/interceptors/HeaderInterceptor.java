package com.binchencoder.skylb.interceptors;

import static com.binchencoder.skylb.constants.GrpcConst.REMOTE_IP_KEY;

import com.binchencoder.skylb.utils.GrpcContextUtils.ContextEntity;
import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import java.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderInterceptor implements ServerInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(HeaderInterceptor.class);

  public static final Context.Key<ContextEntity> CONTEXT_VAL_ENTITY_KEY =
      Context.key("contextValueEntity");

  @Override
  public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {
    SocketAddress remoteAddr = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);

    ContextEntity contextEntity = ContextEntity.newBuilder()
        .setRemoteAddr(remoteAddr)
        .setRemoteIP(headers.get(REMOTE_IP_KEY))
        .build();
    Context context = Context.current().withValue(CONTEXT_VAL_ENTITY_KEY, contextEntity);
    return Contexts.interceptCall(context, call, headers, next);
  }
}
