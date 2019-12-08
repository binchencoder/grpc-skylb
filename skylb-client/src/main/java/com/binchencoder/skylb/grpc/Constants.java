package com.binchencoder.skylb.grpc;

import io.grpc.Metadata;

public class Constants {
  // Key of trace id in GRPC implementation.
  // Being consistent with mdKeyTraceid in letsgo/grpc/metadata.go, so as to make use of
  // trace id among java and golang programs.
  static final String metadataKeyTraceId = "mdkey_traceid";

  static final Metadata.Key<String> correlationIdHeadKey =
      Metadata.Key.of(metadataKeyTraceId, Metadata.ASCII_STRING_MARSHALLER);

  static final String CORRELATION_ID = "X-Request-Id";
}
