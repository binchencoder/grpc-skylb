package com.binchencoder.skylb.common;

import io.opencensus.trace.Span;
import java.util.ArrayList;
import java.util.List;

public class TraceContext {

  private static final String SPAN_LIST_KEY = "spanList";

  public static final String TRACE_ID_KEY = "traceId";

  public static final String SPAN_ID_KEY = "spanId";

  public static final String ANNO_CS = "cs";

  public static final String ANNO_CR = "cr";

  public static final String ANNO_SR = "sr";

  public static final String ANNO_SS = "ss";

  private TraceContext() {
  }

  public static void setTraceId(String traceId) {
    RpcContext.getContext().set(TRACE_ID_KEY, traceId);
  }

  public static String getTraceId() {
    return (String) RpcContext.getContext().get(TRACE_ID_KEY);
  }

  public static String getSpanId() {
    return (String) RpcContext.getContext().get(SPAN_ID_KEY);
  }

  public static void setSpanId(String spanId) {
    RpcContext.getContext().set(SPAN_ID_KEY, spanId);
  }

  @SuppressWarnings("unchecked")
  public static void addSpan(Span span) {
    ((List<Span>) RpcContext.getContext().get(SPAN_LIST_KEY)).add(span);
  }

  @SuppressWarnings("unchecked")
  public static List<Span> getSpans() {
    return (List<Span>) RpcContext.getContext().get(SPAN_LIST_KEY);
  }

  public static void clear() {
    RpcContext.getContext().remove(TRACE_ID_KEY);
    RpcContext.getContext().remove(SPAN_ID_KEY);
    RpcContext.getContext().remove(SPAN_LIST_KEY);
  }

  public static void start() {
    clear();
    RpcContext.getContext().set(SPAN_LIST_KEY, new ArrayList<Span>());
  }
}
