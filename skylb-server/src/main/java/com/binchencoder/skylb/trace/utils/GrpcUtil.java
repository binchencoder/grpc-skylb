package com.binchencoder.skylb.trace.utils;

import io.grpc.Metadata;

public class GrpcUtil {

  /**
   * Metadata key of 'x-context-attachments'
   */
  public static final Metadata.Key<String> GRPC_CONTEXT_ATTACHMENTS =
      Metadata.Key.of("x-context-attachments", Metadata.ASCII_STRING_MARSHALLER);
  /**
   * Metadata key of 'x-context-values'
   */
  public static final Metadata.Key<String> GRPC_CONTEXT_VALUES =
      Metadata.Key.of("x-context-values", Metadata.ASCII_STRING_MARSHALLER);
}
