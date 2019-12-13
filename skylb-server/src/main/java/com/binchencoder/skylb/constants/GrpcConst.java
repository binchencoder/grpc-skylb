package com.binchencoder.skylb.constants;

import io.grpc.Metadata;

public final class GrpcConst {

  /**
   * Metadata key of 'x-forwarded-for'
   */
  public static final Metadata.Key<String> REMOTE_IP_KEY =
      Metadata.Key.of("x-forwarded-for", Metadata.ASCII_STRING_MARSHALLER);
  /**
   * Metadata key of 'x-source'
   */
  public static final Metadata.Key<String> SOURCE_KEY =
      Metadata.Key.of("x-source", Metadata.ASCII_STRING_MARSHALLER);

}
