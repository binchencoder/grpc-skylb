package com.binchencoder.skylb.trace.utils;

import java.util.Formatter;

/**
 * 工具类异常
 */
public class UtilException extends RuntimeException {

  private static final long serialVersionUID = 8247610319171014183L;

  public UtilException(Throwable e) {
    super(getMessage(e), e);
  }

  public UtilException(String message) {
    super(message);
  }

  public UtilException(String messageTemplate, Object... params) {
    super(new Formatter().format(messageTemplate, params).toString());
  }

  public UtilException(String message, Throwable throwable) {
    super(message, throwable);
  }

  public UtilException(Throwable throwable, String messageTemplate, Object... params) {
    super(new Formatter().format(messageTemplate, params).toString(), throwable);
  }

  /**
   * 获得完整消息，包括异常名，消息格式为：{SimpleClassName}: {ThrowableMessage}
   *
   * @param e 异常
   * @return 完整消息
   */
  public static String getMessage(Throwable e) {
    if (null == e) {
      return "null";
    }
    return new Formatter()
        .format("{}: {}", e.getClass().getSimpleName(), e.getMessage()).toString();
  }
}
