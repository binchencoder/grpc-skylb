package com.binchencoder.skylb.trace;

import com.binchencoder.skylb.trace.utils.NetUtil;
import com.google.common.base.Stopwatch;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import zipkin2.Endpoint;
import zipkin2.Span;

public class ZebraServerTracing {

  private static final ZebraServerTracing zebraServerTracing = new ZebraServerTracing();

  public static final ZebraServerTracing getInstance() {
    return zebraServerTracing;
  }

  public Span startTrace(String method) {
    String traceId = (String) RpcContext.getContext().get(TraceContext.TRACE_ID_KEY);
    String parentSpanId = (String) RpcContext.getContext().get(TraceContext.SPAN_ID_KEY);

    String id = UUID.randomUUID().toString();
    TraceContext.start();
    TraceContext.setTraceId(traceId);
    TraceContext.setSpanId(parentSpanId);

    long timestamp = System.currentTimeMillis() * 1000;
    Endpoint endpoint = Endpoint.newBuilder()
        .ip(NetUtil.getLocalhost())
//        .serviceName(EtcdRegistry.serviceName) // // TODO(chenbin) Set current service name.
        .port(50003)
        .build();
    Span providerSpan = Span.newBuilder()
        .id(id)
        .parentId(parentSpanId)
        .traceId(traceId)
//        .name(EtcdRegistry.serviceName) // TODO(chenbin) Set current service name.
        .timestamp(timestamp)
        .localEndpoint(endpoint)
        .addAnnotation(timestamp, TraceContext.ANNO_SR)
        .putTag("method", method)
        .putTag("pkgId", RpcContext.getContext().getAttachment("pkg"))
        .build();
    TraceContext.addSpan(providerSpan);
    return providerSpan;
  }

  public void endTrace(Span span, Stopwatch watch, int code) {
    span = span.toBuilder()
        .addAnnotation(System.currentTimeMillis() * 1000, TraceContext.ANNO_SS)
        .duration(watch.stop().elapsed(TimeUnit.MICROSECONDS))
        .putTag("code", code + "")
        .build();
    TraceAgent traceAgent = new TraceAgent();
    traceAgent.send(TraceContext.getSpans());
  }
}
