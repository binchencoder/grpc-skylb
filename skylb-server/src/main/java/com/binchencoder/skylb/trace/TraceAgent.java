package com.binchencoder.skylb.trace;

import java.util.List;
import zipkin2.Span;

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
