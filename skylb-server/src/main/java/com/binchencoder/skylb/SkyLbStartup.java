package com.binchencoder.skylb;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.binchencoder.common.DurationConverter.DurationConverterInstanceFactory;
import com.binchencoder.common.LevelConverter.LevelConverterInstanceFactory;
import com.binchencoder.skylb.common.ShutdownHookThread;
import com.binchencoder.skylb.config.AbstractConfig;
import com.binchencoder.skylb.config.AppConfig;
import com.binchencoder.skylb.config.LoggerConfig;
import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.grpc.SkyLbServiceImpl;
import com.binchencoder.skylb.hub.SkyLbGraphImpl;
import com.binchencoder.skylb.svcutil.AppUtil;
import com.binchencoder.skylb.utils.Logging;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbStartup {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbStartup.class);

  private static final String DEFAULT_LOGBACK_FILE = "config/logback.xml";

  public static void main(String[] args) {
    SkyLbController controller = createSkyLbController(args);
    boolean initResult = controller.initialize();
    if (!initResult) {
      controller.shutdown();
      System.exit(-3);
    }

    Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(LOGGER, (Callable<Void>) () -> {
      controller.shutdown();
      return null;
    }));

    try {
      controller.start();

      String tip =
          "The SkyLB Server boot successed. gRPC port=" + controller.getServerConfig().getPort();
      LOGGER.info(tip);
      System.out.printf("%s%n", tip);
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  private static SkyLbController createSkyLbController(String[] args) {
    // Parsing the commander the parameters
    ServerConfig serverConfig = new ServerConfig();
    parseCommandArgs(args, serverConfig);

    final SkyLbController controller = new SkyLbController(serverConfig);
    return controller;
  }

  private static void parseCommandArgs(String[] args, ServerConfig serverConfig) {
    LoggerConfig loggerConfig = new LoggerConfig();
    AppConfig appConfig = new AppConfig();
    JCommander commander = JCommander.newBuilder()
        .addConverterInstanceFactory(new DurationConverterInstanceFactory())
        .addConverterInstanceFactory(new LevelConverterInstanceFactory())
        .addObject(EtcdClient.etcdConfig)
        .addObject(appConfig)
        .addObject(serverConfig)
        .addObject(loggerConfig)
        .addObject(SkyLbServiceImpl.config)
        .addObject(SkyLbGraphImpl.config)
        .addCommand(EtcdClient.etcdConfig)
        .addCommand("logger", loggerConfig, "log", "logging")
        .build();
    commander.setCaseSensitiveOptions(false);
    commander.setProgramName("java -jar skylb.jar");
    try {
      commander.parse(args);
    } catch (ParameterException e) {
      e.getJCommander().getConsole().println("Parse args error: " + e.getMessage() + "\n");
      commander.usage();
      System.exit(1);
    }

    if (null != commander.getParsedCommand()) {
      commander.getUsageFormatter().usage(commander.getParsedCommand());
      System.exit(1);
    }

    if (appConfig.getHelp()) {
      commander.usage();
      System.exit(1);
    }

    if (appConfig.isPrintVersion()) {
      commander.getConsole().println(AppUtil.getAppVersion());
      System.exit(1);
    }

    if (appConfig.isPrintLevel()) {
      commander.getConsole().println(Logging.getLevel(Logger.ROOT_LOGGER_NAME).levelStr);
      System.exit(1);
    }

    // ======================Load logback============================
    InputStream is = null;
    try {
      if (null == loggerConfig.getLogbackPath()) {
        is = SkyLbStartup.class.getClassLoader().getResourceAsStream(DEFAULT_LOGBACK_FILE);
      } else {
        is = new FileInputStream(loggerConfig.getLogbackPath().toFile());
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      System.exit(1);
    }
    Logging.configureLogback(is);

    if (loggerConfig.hasLoggerLevel()) {
      Logging.setLevel(loggerConfig.getLoggerLevel());
    }

    // Whether to print the log to the console.
    if (loggerConfig.logToStdout()) {
      Logging.setLogToStdout();
    }

    commander.getConsole().println("Start SKyLB Server ...");
    commander.getConsole().println("JCommander: list all parameters key and values");
    for (Object ob : commander.getObjects()) {
      commander.getConsole().println(((AbstractConfig) ob).toKeyValues());
    }
    commander.getConsole().println("");
  }
}
