package com.binchencoder.skylb.config;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Strings;

@Parameters(separators = "=", commandDescription = "Print logger options")
public class LoggerConfig {

  @Parameter(names = {"--log-level", "-log-level"},
      description = "The application logging level, e.g., ERROR, WARN, INFO, DEBUG, TRACE")
  private String loggerLevel = Level.DEBUG.levelStr;

  @Parameter(names = {"--log-to-stdout", "-log-to-stdout"},
      description = "Whether to print the log to the console.")
  private boolean logToStdout = false;

  public String getLoggerLevel() {
    return loggerLevel.toLowerCase();
  }

  public boolean hasLoggerLevel() {
    return !Strings.isNullOrEmpty(loggerLevel);
  }

  public boolean logToStdout() {
    return logToStdout;
  }
}
