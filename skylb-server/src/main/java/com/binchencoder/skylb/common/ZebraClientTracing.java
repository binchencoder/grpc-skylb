package com.binchencoder.skylb.common;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import zipkin2.Endpoint;
import zipkin2.Span;

public class ZebraClientTracing {

//  public Span startTrace(String method) {
//    String id = /*IdUtils.get() +*/ "";
//    String traceId = null;
//    if (null == TraceContext.getTraceId()) {
//      TraceContext.start();
//      traceId = id;
//    } else {
//      traceId = TraceContext.getTraceId();
//    }
//    long timestamp = System.currentTimeMillis() * 1000;
//    // 注册本地信息
//    Endpoint endpoint = Endpoint.newBuilder().ip(NetUtils.getLocalHost())
//        .serviceName(EtcdRegistry.serviceName)
//        .port(50003).build();
//    // 初始化span
//    Span consumerSpan = Span.newBuilder().localEndpoint(endpoint).id(id).traceId(traceId)
//        .parentId(TraceContext.getSpanId() + "").name(EtcdRegistry.serviceName).timestamp(timestamp)
//        .addAnnotation(timestamp, TraceContext.ANNO_CS).putTag("method", method)
//        .putTag("pkgId", RpcContext.getContext().getAttachment("pkg")).build();
//    // 将tracing id和spanid放到上下文
//    RpcContext.getContext().get().put(TraceContext.TRACE_ID_KEY, consumerSpan.traceId());
//    RpcContext.getContext().get().put(TraceContext.SPAN_ID_KEY, String.valueOf(consumerSpan.id()));
//    return consumerSpan;
//  }

  public void endTrace(Span span, Stopwatch watch, int code) {
    span = span.toBuilder().addAnnotation(System.currentTimeMillis() * 1000, TraceContext.ANNO_CR)
        .duration(watch.stop().elapsed(TimeUnit.MICROSECONDS)).putTag("code", code + "").build();
    TraceAgent traceAgent = new TraceAgent();
    traceAgent.send(TraceContext.getSpans());
  }
}
