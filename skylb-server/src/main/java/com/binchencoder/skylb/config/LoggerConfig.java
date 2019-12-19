package com.binchencoder.skylb.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Strings;

@Parameters(separators = "=", commandDescription = "Print logger options")
public class LoggerConfig {

  @Parameter(names = {"--log-level", "-log-level"},
      description = "The application logging level, e.g., ERROR, WARN, INFO, DEBUG, TRACE")
  private String loggerLevel;

  public String getLoggerLevel() {
    return loggerLevel.toLowerCase();
  }

  public boolean hasLoggerLevel() {
    return !Strings.isNullOrEmpty(loggerLevel);
  }
}
