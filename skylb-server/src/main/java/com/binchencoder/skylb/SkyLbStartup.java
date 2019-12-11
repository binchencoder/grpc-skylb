package com.binchencoder.skylb;

import com.beust.jcommander.JCommander;
import com.binchencoder.skylb.config.EtcdConfig;
import com.binchencoder.skylb.config.ServerConfig;
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

    Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(LOGGER, new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        controller.shutdown();
        return null;
      }
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
    EtcdConfig etcdConfig = new EtcdConfig();
    ServerConfig serverConfig = new ServerConfig();
    JCommander commander = JCommander.newBuilder()
        .addObject(etcdConfig)
        .addObject(serverConfig)
        .build();
    commander.parse(args);
    commander.usage();

    final SkyLbController controller = new SkyLbController(etcdConfig, serverConfig);
    return controller;
  }
}
