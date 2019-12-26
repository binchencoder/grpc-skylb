package com.binchencoder.skylb.examples.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.layout.TTLLLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.base.Preconditions;
import java.io.InputStream;
import org.slf4j.LoggerFactory;

public final class Logging {

  public static void setLevel(Level level) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(level);
  }

  public static Level getLevel(String name) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    return loggerContext.getLogger(name).getLevel();
  }

  public static void setLogToStdout() {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

    final ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
    ca.setContext(lc);
    ca.setName("console");
    final LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
    encoder.setContext(lc);
    final TTLLLayout layout = new TTLLLayout();
    layout.setContext(lc);
    layout.start();
    encoder.setLayout(layout);
    ca.setEncoder(encoder);
    ca.start();
    final Logger rootLogger = lc.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(ca);
  }

  public static void configureLogback(InputStream is) throws JoranException {
    Preconditions.checkNotNull(null != is);
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator configurator = new JoranConfigurator();
    lc.reset();
    configurator.setContext(lc);
    try {
      configurator.doConfigure(is);
    } catch (JoranException e) {
      e.printStackTrace();
      throw e;
    }
  }
}
