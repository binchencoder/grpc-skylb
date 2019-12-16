package com.binchencoder.skylb.config;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class AppConfig {
  @Parameter(names = {"--help", "-help", "--h", "-h"},
      description = "Print command line help", help = true)
  private boolean help = false;

  @Parameter(names = {"--version", "-version", "--v", "-v"},
      description = "Print application's version")
  private boolean printVersion = false;

  @Parameter(names = {"--level", "-level", "--l", "-l"},
      description = "Print application's logger root level")
  private boolean printLevel = false;

  public boolean getHelp() {
    return help;
  }

  public boolean isPrintVersion() {
    return this.printVersion;
  }

  public boolean isPrintLevel() {
    return printLevel;
  }
}
