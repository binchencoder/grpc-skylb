package com.binchencoder.cmd.skylb;

import static com.binchencoder.skylb.config.LoggerConfig.DEFAULT_LOGBACK_FILE;

import ch.qos.logback.core.joran.spi.JoranException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.InetAddressConverter;
import com.beust.jcommander.internal.Lists;
import com.binchencoder.common.jcommander.DurationConverter.DurationConverterInstanceFactory;
import com.binchencoder.common.jcommander.LevelConverter.LevelConverterInstanceFactory;
import com.binchencoder.skylb.config.AbstractConfig;
import com.binchencoder.skylb.config.AppConfig;
import com.binchencoder.skylb.config.LoggerConfig;
import com.binchencoder.skylb.config.MetricsConfig;
import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.grpc.SkyLbServiceImpl;
import com.binchencoder.skylb.hub.EndpointsHub;
import com.binchencoder.skylb.hub.EndpointsHubImpl;
import com.binchencoder.skylb.hub.SkyLbGraph;
import com.binchencoder.skylb.hub.SkyLbGraphImpl;
import com.binchencoder.skylb.interceptors.HeaderInterceptor;
import com.binchencoder.skylb.interceptors.HeaderServerInterceptor;
import com.binchencoder.skylb.lameduck.LameDuck;
import com.binchencoder.skylb.monitoring.AbstractProducer;
import com.binchencoder.skylb.monitoring.NoneProducer;
import com.binchencoder.skylb.monitoring.SkyLbHTTPServer;
import com.binchencoder.skylb.monitoring.SkyLbMetrics;
import com.binchencoder.skylb.prefix.InitPrefix;
import com.binchencoder.skylb.svcutil.AppUtil;
import com.binchencoder.skylb.utils.Logging;
import com.binchencoder.util.StoppableTask;
import com.binchencoder.util.TaskManager;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.StatusRuntimeException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbContext.class);

  // SkyLB config
  private final AppConfig appConfig = new AppConfig();
  private final MetricsConfig metricsConfig = new MetricsConfig();
  private final ServerConfig serverConfig = new ServerConfig();
  private final LoggerConfig loggerConfig = new LoggerConfig();

  // SkyLB metrics
  private final SkyLbMetrics metrics;
  private AbstractProducer producer;

  // SKyLB tasks
  private final TaskManager taskManager;
  private volatile Exception error;

  private Thread terminationThread;

  private EtcdClient etcdClient;
  private EndpointsHub endpointsHub;
  private SkyLbGraph skyLbGraph;
  private LameDuck lameDuck;

  private Server server;

  public SkyLbContext(String[] args)
      throws JoranException, UnknownHostException, URISyntaxException, StatusRuntimeException {
    // Parsing the commander the parameters
    this.parseCommandArgs(args);

    this.etcdClient = new EtcdClient();
    this.lameDuck = new LameDuck(etcdClient);
    this.endpointsHub = new EndpointsHubImpl(etcdClient, lameDuck, serverConfig);
    this.skyLbGraph = new SkyLbGraphImpl(etcdClient);

    this.metrics = new SkyLbMetrics(metricsConfig);
    this.taskManager = new TaskManager();
  }

  public void start() throws IOException {
    SkyLbServiceImpl skyLbService = new SkyLbServiceImpl(endpointsHub, lameDuck, skyLbGraph);

    // Bind server interceptors
    final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(serverConfig.getPort());
    serverBuilder.addService(this.bindInterceptors(skyLbService.bindService()));

    this.server = serverBuilder.build().start();
    this.startDaemonAwaitThread();

    SkyLbHTTPServer.startIfRequired(this);

    // Initializes ETCD prefix keys.
    InitPrefix.newInstance(etcdClient);
  }

  public void addTask(StoppableTask task) {
    this.taskManager.add(task);
  }

  public Exception getError() {
    return error;
  }

  public Thread terminate() {
    return terminate(null);
  }

  public Thread terminate(Exception error) {
    if (this.error == null) {
      this.error = error;
    }

    if (taskManager.requestStop()) {
      if (this.error != null) {
        error.printStackTrace();
      }
      this.terminationThread = spawnTerminateThread();
    }
    return this.terminationThread;
  }

  public AbstractProducer getProducer() {
    if (this.producer != null) {
      return this.producer;
    }
    switch (this.metricsConfig.producerType) {
      case "none":
        this.producer = new NoneProducer(this);
        break;
      default:
        throw new RuntimeException("Unknown producer type: " + this.metricsConfig.producerType);
    }

    StoppableTask task = null;
    if (producer != null) {
      task = producer.getStoppableTask();
    }
    if (task != null) {
      addTask(task);
    }
    return this.producer;
  }

  public EtcdClient getEtcdClient() {
    return etcdClient;
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  public MetricsConfig getMetricsConfig() {
    return metricsConfig;
  }

  public SkyLbMetrics getMetrics() {
    return metrics;
  }

  private void parseCommandArgs(String[] args) throws JoranException, URISyntaxException {
    JCommander commander = JCommander.newBuilder()
        .addConverterInstanceFactory(new DurationConverterInstanceFactory())
        .addConverterInstanceFactory(new LevelConverterInstanceFactory())
        .addConverterInstanceFactory((parameter, forType, optionName) -> {
          if (forType == InetAddress.class) {
            return new InetAddressConverter();
          }
          return null;
        })
        .addObject(EtcdClient.etcdConfig)
        .addObject(appConfig)
        .addObject(serverConfig)
        .addObject(loggerConfig)
        .addObject(SkyLbServiceImpl.config)
        .addObject(SkyLbGraphImpl.config)
        .addObject(metricsConfig)
        .addCommand(EtcdClient.etcdConfig)
        .addCommand(metricsConfig)
        .addCommand("logger", loggerConfig, "log", "logging")
        .build();
    commander.setCaseSensitiveOptions(false);
    commander.setProgramName("java -jar skylb.jar");
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
        is = SkyLbStartup.class.getClassLoader().getResourceAsStream(DEFAULT_LOGBACK_FILE);
      } else {
        is = new FileInputStream(loggerConfig.getLogbackPath().toFile());
      }
    } catch (FileNotFoundException e) {
      // catch FileNotFoundException explicitly as well to provide more information to the user
      LOGGER.error(
          "FileNotFound issue with logback xml, check parameter of --logback-path that the configured logback xml file exists");
      LOGGER.error("FileNotFoundException: {}", e.getLocalizedMessage());
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

    commander.getConsole().println("Start SKyLB Server ...");
    commander.getConsole().println("JCommander: list all parameters key and values");
    for (Object ob : commander.getObjects()) {
      commander.getConsole().println(((AbstractConfig) ob).toKeyValues());
    }
    commander.getConsole().println("");
  }

  private void shutdown(AtomicBoolean complete) {
    LOGGER.info("Shutting down gRPC server ...");

    try {
      taskManager.stop(this.error);

      this.endpointsHub.close();
      this.skyLbGraph.close();

      Optional.ofNullable(server).ifPresent(Server::shutdown);
      LOGGER.info("SkyLb gRPC server stopped.");

      complete.set(true);
    } catch (Exception e) {
      LOGGER.error("Exception occurred during shutdown:", e);
    }
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread = new Thread(() -> {
      try {
        SkyLbContext.this.server.awaitTermination();
      } catch (InterruptedException e) {
        LOGGER.error("gRPC server stopped.", e);
      }
    });

    awaitThread.setDaemon(false);
    awaitThread.start();
  }

  // Bind server interceptors
  private ServerServiceDefinition bindInterceptors(ServerServiceDefinition serviceDefinition) {
    List<ServerInterceptor> interceptors = Lists.newArrayList(HeaderServerInterceptor.instance(),
        HeaderInterceptor.instance());
    return ServerInterceptors.intercept(serviceDefinition, interceptors);
  }

  private Thread spawnTerminateThread() {
    // Because terminate() may be called from a task thread
    // which won't end until we let its event loop progress,
    // we need to perform termination in a new thread
    final AtomicBoolean shutdownComplete = new AtomicBoolean(false);
    final SkyLbContext self = this;
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        // Spawn an inner thread to perform shutdown
        final Thread shutdownThread = new Thread(new Runnable() {
          @Override
          public void run() {
            self.shutdown(shutdownComplete);
          }
        }, "shutdownThread");
        shutdownThread.start();

        // wait for its completion, timing out after 10s
        try {
          shutdownThread.join(10000L);
        } catch (InterruptedException e) {
          // ignore
        }

        LOGGER.debug("Shutdown complete: {}", shutdownComplete.get());
        if (!shutdownComplete.get()) {
          LOGGER.error("Shutdown stalled - forcefully killing skylb process");
          if (self.error != null) {
            LOGGER.error("Termination reason: {}", self.error);
          }
          Runtime.getRuntime().halt(1);
        }
      }
    }, "shutdownMonitor");
    thread.setDaemon(false);
    thread.start();
    return thread;
  }
}
