package com.binchencoder.skylb;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.beust.jcommander.JCommander;
import com.binchencoder.skylb.common.ShutdownHookThread;
import com.binchencoder.skylb.config.AppConfig;
import com.binchencoder.skylb.config.LoggerConfig;
import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.grpc.SkyLbServiceImpl;
import com.binchencoder.skylb.hub.SkyLbGraphImpl;
import com.binchencoder.skylb.svcutil.AppUtil;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbStartup {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbStartup.class);

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
          "The SkyLB Server boot success. gRPC port=" + controller.getServerConfig().getPort();
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
        .addObject(EtcdClient.etcdConfig)
        .addObject(appConfig)
        .addObject(serverConfig)
        .addObject(loggerConfig)
        .addObject(SkyLbServiceImpl.config)
        .addObject(SkyLbGraphImpl.config)
//        .addCommand(EtcdClient.etcdConfig)
        .build();
    commander.setProgramName("skylb", "SkyLB Server");
    commander.parse(args);
    if (appConfig.getHelp()) {
      commander.usage();
      System.exit(1);
    }

    if (appConfig.isPrintVersion()) {
      commander.getConsole().println(AppUtil.getAppVersion());
      System.exit(1);
    }

    if (appConfig.isPrintLevel()) {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      commander.getConsole().println(loggerContext.getLogger("root").getLevel().levelStr);
      System.exit(1);
    }

    if (loggerConfig.hasLoggerLevel()) {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      loggerContext.getLogger("root").setLevel(Level.valueOf(loggerConfig.getLoggerLevel()));
    }
  }
}
