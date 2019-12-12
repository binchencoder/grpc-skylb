package com.binchencoder.skylb.common;

import io.opencensus.trace.Span;
import java.util.List;

public class TraceAgent {

//  private GrpcProperties grpcProperties;
//  private KafkaSender sender;
//  private AsyncReporter<zipkin2.Span> report;

  public TraceAgent() {
//    grpcProperties = SpringContextUtils.getBean(GrpcProperties.class);
//    sender = KafkaSender.newBuilder().bootstrapServers(grpcProperties.getCallChainUpdAddr())
//        .topic("zipkin").encoding(Encoding.JSON).build();
//    report = AsyncReporter.builder(sender).build();
  }

  public void send(final List<Span> spans) {
//    spans.forEach(item -> {
//      report.report(item);
//    });
  }

}
