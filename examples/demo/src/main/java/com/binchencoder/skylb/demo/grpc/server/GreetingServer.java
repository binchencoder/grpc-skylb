package com.binchencoder.skylb.demo.grpc.server;

import static com.binchencoder.skylb.demo.config.LoggerConfig.DEFAULT_LOGBACK_FILE;

import ch.qos.logback.core.joran.spi.JoranException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.InetAddressConverter;
import com.binchencoder.skylb.SkyLBServiceReporter;
import com.binchencoder.skylb.demo.Configuration;
import com.binchencoder.skylb.demo.config.AbstractConfig;
import com.binchencoder.skylb.demo.config.AppConfig;
import com.binchencoder.skylb.demo.config.LevelConverter.LevelConverterInstanceFactory;
import com.binchencoder.skylb.demo.config.LoggerConfig;
import com.binchencoder.skylb.demo.config.ServerConfig;
import com.binchencoder.skylb.demo.grpc.client.GreetingClient;
import com.binchencoder.skylb.demo.util.AppUtil;
import com.binchencoder.skylb.demo.util.Logging;
import com.binchencoder.skylb.grpc.ServerTemplate;
import com.binchencoder.skylb.grpchealth.JinHealthServiceImpl;
import com.binchencoder.skylb.grpchealth.JinHealthServiceInterceptor;
import com.binchencoder.skylb.metrics.MetricsServerInterceptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreetingServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(GreetingServer.class);

  // config
  private static final AppConfig appConfig = new AppConfig();
  private static final ServerConfig serverConfig = new ServerConfig();
  private static final LoggerConfig loggerConfig = new LoggerConfig();

  private String serviceName;
  private String portName;
  private int port;

  private SkyLBServiceReporter reporter;
  private Server server;

  void start(String skylbUri, String serviceName, String portName, int port) throws Exception {
    this.serviceName = serviceName;
    this.portName = portName;
    this.port = port;

    // Recommend using new way of initializing grpc service.
    boolean newWay = true;
    if (newWay) {
      this.server = ServerTemplate.create(this.port, new DemoGrpcImpl(this.port), serviceName)
          .build()
          .start();
    } else {
      this.server = ServerBuilder.forPort(this.port)
          .addService(ServerInterceptors.intercept(
              // Enable service provider.
              new DemoGrpcImpl(this.port),
              // Enable metrics.
              MetricsServerInterceptor.create(
                  com.binchencoder.skylb.metrics.Configuration.allMetrics(), serviceName)))
          // Enable grpc health checking.
          .addService(ServerInterceptors.intercept(new JinHealthServiceImpl(),
              new JinHealthServiceInterceptor()))
          .build()
          .start();
    }
    LOGGER.info("Target server {}@:{} started.", serviceName, this.port);
    this.reporter = ServerTemplate.reportLoad(skylbUri, serviceName, portName, this.port);

    LOGGER.info("Target server {}@:{}-{} registered into skylb service {}.",
        serviceName, port, portName, skylbUri);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        GreetingServer.this.stop();
        System.err.println("Target server " + serviceName + "@" + port + " shut down");
      }
    });
  }

  void stop() {
    this.reporter.shutdown();
    if (server != null && !server.isShutdown()) {
      this.server.shutdownNow();
      LOGGER.info("GreetingServer {}@:{}-{} stopped!", this.serviceName, this.portName, this.port);
    }
  }

  public static void main(String[] args) throws Exception {
    // Parsing the commander the parameters
    parseCommandArgs(args);

    // (Optional) Start prometheus.
//    MonitorService.getInstance().startPrometheus(
//        Configuration.METRICS_IP, Configuration.METRICS_PORT, Configuration.METRICS_PATH);

    GreetingServer greetingServer = new GreetingServer();
    greetingServer.start(serverConfig.getSkylbAddr(), Configuration.SERVER_SERVICE_NAME,
        serverConfig.getPortName(), serverConfig.getPort());
    LOGGER.info("Binchencoder skylb greeting demo Server is running!");

    Thread.sleep(TimeUnit.MINUTES.toMillis(60));
  }

  private static void parseCommandArgs(String[] args) throws JoranException {
    JCommander commander = JCommander.newBuilder()
        .addConverterInstanceFactory(new LevelConverterInstanceFactory())
        .addConverterInstanceFactory((parameter, forType, optionName) -> {
          if (forType == InetAddress.class) {
            return new InetAddressConverter();
          }
          return null;
        })
        .addObject(appConfig)
        .addObject(serverConfig)
        .addObject(loggerConfig)
        .addCommand("LOGGER", loggerConfig, "log", "logging")
        .build();
    commander.setCaseSensitiveOptions(false);
    commander.setProgramName("java -jar echoclient.jar");
    try {
      commander.parse(args);
    } catch (ParameterException e) {
      e.getJCommander().getConsole().println("Parse args error: " + e.getMessage() + "\n");
      commander.usage();

      throw e;
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
      commander.getConsole().println(AppUtil.getVersion());
      System.exit(1);
    }

    if (appConfig.isPrintLevel()) {
      commander.getConsole().println(Logging.getLevel(Logger.ROOT_LOGGER_NAME).levelStr);
      System.exit(1);
    }

    /** ======================Load logback============================ **/
    InputStream is = null;
    try {
      if (null == loggerConfig.getLogbackPath()) {
        is = GreetingClient.class.getClassLoader().getResourceAsStream(DEFAULT_LOGBACK_FILE);
      } else {
        is = new FileInputStream(loggerConfig.getLogbackPath().toFile());
      }
    } catch (FileNotFoundException e) {
      // catch FileNotFoundException explicitly as well to provide more information to the user
      LOGGER.error(
          "FileNotFound issue with logback xml, check parameter of --logback-path that the configured logback xml file exists");
      LOGGER.error("FileNotFoundException: " + e.getLocalizedMessage());
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
    /** ======================Load logback============================ **/

    commander.getConsole().println("Start Greeting Server ...");
    commander.getConsole().println("JCommander: list all parameters key and values");
    for (Object ob : commander.getObjects()) {
      commander.getConsole().println(((AbstractConfig) ob).toKeyValues());
    }
    commander.getConsole().println("");
  }
}