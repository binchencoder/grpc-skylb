package com.binchencoder.skylb.util;


import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import com.binchencoder.grpc.errors.Errors.Error;
import com.binchencoder.grpc.errors.Errors.ErrorCode;

public class GrpcErrorUtils {

  private static final Logger logger = LoggerFactory.getLogger(GrpcErrorUtils.class);

  /**
   * 拿到StatusRuntimeException后 调用 responseObserver.onError(ex); <br>
   * 注意:调用onError后 call already closed 不需要再调用onCompleted
   * 
   * @param error
   * @param status
   * @return
   *
   * @author linds
   */
  public static StatusRuntimeException fromGrpcError(Error error, Status status) {
    if (error != null) {
      try {
        return new StatusRuntimeException(
            status.withDescription(JsonFormat.printer().print(error)));
      } catch (InvalidProtocolBufferException e) {
        logger.error("transform to GrpcError error for {}", error, e);
      }
    }
    return new StatusRuntimeException(status);
  }

  /**
   * 通过GRPC的StatusRuntimeException返回ErrorCode
   * @author linds
   */
  public static ErrorCode getGrpcErrorCode(StatusRuntimeException ex) {
    String description = ex.getStatus().getDescription();
    if (StringUtils.isNotBlank(description)) {
      Error.Builder builder = Error.newBuilder();
      try {
        JsonFormat.parser().merge(new StringReader(description), builder);
        return builder.build().getCode();
      } catch (IOException e) {
        logger.error("JsonFormat to ErrorCode error for {}", description, e);
      }
    }
    return ErrorCode.NONE;
  }
}
