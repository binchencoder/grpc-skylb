package com.binchencoder.skylb.demo.grpc.client;

import static com.binchencoder.skylb.demo.config.LoggerConfig.DEFAULT_LOGBACK_FILE;

import ch.qos.logback.core.joran.spi.JoranException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.InetAddressConverter;
import com.binchencoder.skylb.SkyLBConst;
import com.binchencoder.skylb.SkyLBNameResolverFactory;
import com.binchencoder.skylb.balancer.consistenthash.ConsistentHashLoadBalancerFactory;
import com.binchencoder.skylb.balancer.roundrobin.RoundRobinLoadBalancerFactory;
import com.binchencoder.skylb.demo.Configuration;
import com.binchencoder.skylb.demo.config.AbstractConfig;
import com.binchencoder.skylb.demo.config.AppConfig;
import com.binchencoder.skylb.demo.config.LevelConverter.LevelConverterInstanceFactory;
import com.binchencoder.skylb.demo.config.LoggerConfig;
import com.binchencoder.skylb.demo.config.ServerConfig;
import com.binchencoder.skylb.demo.proto.DemoGrpc;
import com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingRequest;
import com.binchencoder.skylb.demo.proto.GreetingProtos.GreetingResponse;
import com.binchencoder.skylb.demo.util.AppUtil;
import com.binchencoder.skylb.demo.util.Logging;
import com.binchencoder.skylb.grpc.Channels;
import com.binchencoder.skylb.grpc.ClientTemplate;
import com.binchencoder.skylb.metrics.MetricsClientInterceptor;
import io.grpc.LoadBalancer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates various methods to init grpc channel.
 *
 * Recommend using ClientTemplate.createChannel(skylbUri, ...
 */
enum DemoGrpcInitMethod {
  // Use raw grpc.
  RAW,

  //Use ClientTemplate.createChannel(target,...
  WRAP1,

  // Use ClientTemplate.createChannel(skylbUri, ...
  WRAP2
}

/**
 * Demonstrates usage of load balancers.
 */
enum DemoLBType {
  RoundRobin,
  ConsistentHash
}

/**
 * Demonstrates usage of skylb uri types.
 */
enum DemoAddrType {
  SkyLB,
  Direct,
  SkyLBAndDirect
}

public class GreetingClient {

  private static Logger LOGGER = LoggerFactory.getLogger(GreetingClient.class);

  // config
  private static final AppConfig appConfig = new AppConfig();
  private static final ServerConfig serverConfig = new ServerConfig();
  private static final LoggerConfig loggerConfig = new LoggerConfig();

  private ManagedChannel channel;
  private Channels channels;
  private URI uri;

  DemoGrpcInitMethod demoInitMethod = DemoGrpcInitMethod.WRAP2;
  DemoLBType demoLbType = null;

  /**
   * Construct client connecting to Echo server at {@code host:port}.
   */
  // 首先, 我们需要为stub创建一个grpc的channel, 指定我们连接服务端的地址和端口
  // 使用ManagedChannelBuilder方法来创建channel
  public GreetingClient() {
    demoLbType = DemoLBType
        .valueOf(System.getProperty("demo-lb-type", DemoLBType.RoundRobin.toString()));
    LOGGER.info("InitMethod:{} lbType:{}", demoInitMethod, demoLbType);

    // Choose load balancer (optional)
    LoadBalancer.Factory lb = null;
    String loadBalancerDesc = SkyLBConst.JG_ROUND_ROBIN;
    switch (demoLbType) {
      case RoundRobin:
        loadBalancerDesc = SkyLBConst.JG_ROUND_ROBIN;
        lb = RoundRobinLoadBalancerFactory.getInstance();
        break;
      case ConsistentHash:
        loadBalancerDesc = SkyLBConst.JG_CONSISTEN_HASH;
        lb = ConsistentHashLoadBalancerFactory.getInstance();
        break;
      // (Here and following switch-cases omit "default" branch for simplicity)
    }

    String callerServiceName = Configuration.CLIENT_SERVICE_NAME;
    String calleeServiceName = Configuration.SERVER_SERVICE_NAME;

    // Choose how to init grpc.
    switch (demoInitMethod) {
      case RAW: {
        String target = serverConfig.getSkylbAddr() + "/" + Configuration.SERVER_SERVICE_NAME
            + "?portName=" + serverConfig.getPortName();
        // skylb://skylb-server1:port1,skylb-server2:port2,.../serviceName?portName=myPort

        this.channel = ManagedChannelBuilder
            // 1. forTarget
            .forTarget(target)
            // 2. use binchencoder resolver
            .nameResolverFactory(SkyLBNameResolverFactory.getInstance(callerServiceName))
            .defaultLoadBalancingPolicy(loadBalancerDesc)
            .intercept(MetricsClientInterceptor.create(
                com.binchencoder.skylb.metrics.Configuration.allMetrics(),
                calleeServiceName, callerServiceName))
            // Set idleTimeout to several seconds to reproduce channel idle behavior.
            //.idleTimeout(31, TimeUnit.DAYS)
            .usePlaintext()
            .build();
      }
      break;

      case WRAP1: {
        DemoAddrType demoAddrType = DemoAddrType.SkyLB;
        String target = null;
        switch (demoAddrType) {
          case SkyLB:
            target = serverConfig.getSkylbAddr() + "/" + Configuration.SERVER_SERVICE_NAME
                + "?portName=" + serverConfig.getPortName();
            // skylb://skylb-server1:port1,skylb-server2:port2,.../serviceName?portName=myPort
            break;
          case Direct:
            target = "direct://127.0.0.1:" + serverConfig.getPort();
            // direct://127.0.0.1:50001
            break;
          case SkyLBAndDirect:
            LOGGER.error("Not supported");
            System.exit(2);
            break;
        }

        this.channels = ClientTemplate
            .createChannel(target, callerServiceName, calleeServiceName, lb);
        // You can also omit lb, to use default RoundRobinLoadBalancerFactory, i.e:
        if (false) {
          this.channels = ClientTemplate
              .createChannel(target, callerServiceName, calleeServiceName);
          // (This line of code is to ensure the method compiles)
        }
      }
      break;

      case WRAP2: {
        DemoAddrType demoAddrType = DemoAddrType.SkyLB;
        String skylbUri = null;
        switch (demoAddrType) {
          case SkyLB:
            skylbUri = serverConfig.getSkylbAddr();
            // skylb://192.168.38.6:1900
            break;
          case Direct:
            skylbUri = "direct://" + calleeServiceName
                + ":127.0.0.1:" + serverConfig.getPort();
            // direct://shared-test-server-service:127.0.0.1:50001
            break;
          case SkyLBAndDirect:
            skylbUri = serverConfig.getSkylbAddr() + ";"
                + "direct://" + calleeServiceName
                + ":127.0.0.1:" + serverConfig.getPort();
            // skylb://192.168.38.6:1900;direct://shared-test-server-service:127.0.0.1:50001
            break;
        }
        this.channels = ClientTemplate.createChannel(skylbUri,
            calleeServiceName, null, null,
            callerServiceName, lb);
        // Parameter lb can be omitted, whose default value is RoundRobinLoadBalancerFactory.

        // (This code block is to guarantee the alternative createChannel method doesn't get broken
        // by accident)
        if (false) {
          this.channels = ClientTemplate.createChannel(skylbUri,
              calleeServiceName, null, null,
              callerServiceName);
        }
      }
      break;
    }
  }

  public void testService() {
    DemoGrpc.DemoBlockingStub blockingStub = null;
    switch (demoInitMethod) {
      case RAW:
        blockingStub = DemoGrpc.newBlockingStub(this.channel);
        break;
      case WRAP1:
      case WRAP2:
        blockingStub = DemoGrpc.newBlockingStub(this.channels.getChannel());
        break;
    }

    for (int i = 0; i < Configuration.TEST_COUNT; i++) {
      try {
        GreetingRequest request = GreetingRequest.newBuilder()
            .setName("GC " + Calendar.getInstance().get(Calendar.SECOND)).build();
        LOGGER.info("Hello request: {}", request.getName());
        GreetingResponse response = null;
        switch (demoLbType) {
          case RoundRobin:
            response = blockingStub
                .withDeadlineAfter(Configuration.DEADLINE_FOR_TEST, TimeUnit.MILLISECONDS)
                .greeting(request);
            break;
          case ConsistentHash:
            response = blockingStub
                .withOption(ConsistentHashLoadBalancerFactory.HASHKEY, "valueOfYourKey")
                .withDeadlineAfter(Configuration.DEADLINE_FOR_TEST, TimeUnit.MILLISECONDS)
                .greeting(request);
            break;
        }
        LOGGER.info("Hello response: {}", response.getGreeting());
      } catch (StatusRuntimeException e) {
        LOGGER.error("Hello error:", e);
      } catch (RuntimeException e) {
        LOGGER.error("Hello Error", e);

        i--;
      } catch (Throwable e) {
        LOGGER.error("Err", e);
      } finally {
        try {
          Thread.sleep(3000);
          // To test idle timeout, sleep 3s or longer, combined with
          // ManagedChannelBuilder...idleTimeout(2, TimeUnit.SECONDS).
        } catch (InterruptedException e) {
          LOGGER.warn("sleep was interrupted.");
        }
      }
    }

    switch (demoInitMethod) {
      case RAW:
        this.channel.shutdown();
        break;
      case WRAP1:
      case WRAP2:
        this.channels.getOriginChannel().shutdown();
        break;
    }
    LOGGER.info("Greeting client shutdown.");
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting.
   */
  public static void main(String[] args) throws InterruptedException, JoranException {
    // Parsing the commander the parameters
    parseCommandArgs(args);

    // Start prometheus (optional).
//    MonitorService.getInstance().startPrometheus(
//        Configuration.METRICS_IP,
//        // Make metrics port different from GreetingServer so as to void port conflict.
//        Configuration.METRICS_PORT + 1,
//        Configuration.METRICS_PATH);

    GreetingClient client = new GreetingClient();

    // Wait a while for resolving to complete.
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
      e.printStackTrace();
    }

    client.testService();
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

    commander.getConsole().println("Start Echo Client ...");
    commander.getConsole().println("JCommander: list all parameters key and values");
    for (Object ob : commander.getObjects()) {
      commander.getConsole().println(((AbstractConfig) ob).toKeyValues());
    }
    commander.getConsole().println("");
  }
}