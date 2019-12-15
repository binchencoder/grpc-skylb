package com.binchencoder.skylb;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.beust.jcommander.JCommander;
import com.binchencoder.skylb.config.LoggerConfig;
import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.grpc.SkyLbServiceImpl;
import com.binchencoder.skylb.svcutil.ShutdownHookThread;
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
    JCommander commander = JCommander.newBuilder()
        .addObject(EtcdClient.etcdConfig)
        .addObject(serverConfig)
        .addObject(loggerConfig)
        .addObject(SkyLbServiceImpl.config)
        .build();
    commander.setProgramName("SkyLB", "SkyLB Server");
    commander.parse(args);
    if (serverConfig.getHelp()) {
      commander.usage();
      System.exit(1);
    }

    if (loggerConfig.hasLoggerLevel()) {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      loggerContext.getLogger("root").setLevel(Level.valueOf(loggerConfig.getLoggerLevel()));
    }
  }
}
