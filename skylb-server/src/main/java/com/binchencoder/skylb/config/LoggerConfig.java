package com.binchencoder.skylb.config;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.nio.file.Path;

@Parameters(separators = "=", commandDescription = "Print logger options")
public class LoggerConfig extends AbstractConfig {

  @Parameter(names = {"--log-level", "-log-level"},
      description = "The application logging level, e.g., ERROR, WARN, INFO, DEBUG, TRACE")
  private Level loggerLevel = Level.DEBUG;

  @Parameter(names = {"--log-to-stdout", "-log-to-stdout"},
      description = "Whether to print the log to the console.")
  private boolean logToStdout = false;

  @Parameter(names = {"--logback-path", "-logback-path"},
      description = "Reload the logback XML path.")
  private Path logbackPath;

  public Level getLoggerLevel() {
    return loggerLevel;
  }

  public boolean hasLoggerLevel() {
    return null != loggerLevel;
  }

  public boolean logToStdout() {
    return logToStdout;
  }

  public Path getLogbackPath() {
    return logbackPath;
  }

  @Override
  public String toKeyValues() {
    return new StringBuilder()
        .append("--log-level").append("=").append(this.getLoggerLevel().levelStr).append("\n")
        .append("--log-to-stdout").append("=").append(this.logToStdout()).append("\n")
        .append("--logback-path").append("=").append(this.getLogbackPath().toString())
        .toString();
  }
}
