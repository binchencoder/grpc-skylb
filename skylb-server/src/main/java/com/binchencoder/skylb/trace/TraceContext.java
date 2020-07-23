package com.binchencoder.skylb.trace;

import java.util.ArrayList;
import java.util.List;
import zipkin2.Span;

public class TraceContext {

  private static final String SPAN_LIST_KEY = "spanList";

  public static final String TRACE_ID_KEY = "traceId";

  public static final String SPAN_ID_KEY = "spanId";

  // 客户端发起请求，标志Span的开始
  public static final String ANNO_CS = "cs";
  // 客户端接收到服务端响应内容，标志着Span的结束，其中cr - ss则为网络延迟和时钟抖动
  public static final String ANNO_CR = "cr";
  // 服务端接收到请求，并开始处理内部事务，其中sr - cs则为网络延迟和时钟抖动
  public static final String ANNO_SR = "sr";
  // 服务端处理完请求，返回响应内容，其中ss - sr则为服务端处理请求耗时
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
