package com.binchencoder.skylb.examples.echo.client;

import static com.binchencoder.skylb.examples.config.LoggerConfig.DEFAULT_LOGBACK_FILE;

import ch.qos.logback.core.joran.spi.JoranException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.InetAddressConverter;
import com.binchencoder.easegw.examples.EchoServiceGrpc;
import com.binchencoder.easegw.examples.ExamplesProto.SimpleMessage;
import com.binchencoder.grpc.data.DataProtos.ServiceId;
import com.binchencoder.grpc.utils.ServiceNameUtil;
import com.binchencoder.skylb.examples.config.AbstractConfig;
import com.binchencoder.skylb.examples.config.AppConfig;
import com.binchencoder.skylb.examples.config.LevelConverter.LevelConverterInstanceFactory;
import com.binchencoder.skylb.examples.config.LoggerConfig;
import com.binchencoder.skylb.examples.config.ServerConfig;
import com.binchencoder.skylb.examples.util.AppUtil;
import com.binchencoder.skylb.examples.util.Logging;
import com.binchencoder.skylb.grpc.ClientTemplate;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoClient {

  private static Logger LOGGER = LoggerFactory.getLogger(EchoClient.class);

  // config
  private static final AppConfig appConfig = new AppConfig();
  private static final ServerConfig serverConfig = new ServerConfig();
  private static final LoggerConfig loggerConfig = new LoggerConfig();
  private static final EchoConfig echoConfig = new EchoConfig();

  private static ManagedChannel channel;
  private static EchoServiceGrpc.EchoServiceBlockingStub blockingStub;

  /**
   * Construct client connecting to Echo server at {@code host:port}.
   */
  // 首先, 我们需要为stub创建一个grpc的channel, 指定我们连接服务端的地址和端口
  // 使用ManagedChannelBuilder方法来创建channel
  public EchoClient() {
    channel = ClientTemplate.createChannel(echoConfig.getSkylbAddr(),
        ServiceNameUtil.toString(ServiceId.CUSTOM_EASE_GATEWAY_TEST),
        "grpc", null,
        ServiceNameUtil.toString(ServiceId.SERVICE_NONE)).getOriginChannel();

    blockingStub = EchoServiceGrpc.newBlockingStub(channel);
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting.
   */
  public static void main(String[] args) throws InterruptedException, JoranException {
    // Parsing the commander the parameters
    parseCommandArgs(args);

    EchoClient client = new EchoClient();

//    CountDownLatch latch = new CountDownLatch(1);
    try {
      int times = 0;
      while (true) {
        for (int i = 0; i < 100; i++) {
          new Thread(() -> {
            client.echo();
          }).start();
        }
        ++times;
        Thread.sleep(times * 10);
      }

//      latch.await();
//      latch.countDown();
    } finally {
      client.shutdown();
    }
  }

  private void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  private void echo() {
    LOGGER.info("Will try to echo " + echoConfig.getEchoId() + "...");

    long start = System.currentTimeMillis();
    SimpleMessage request = SimpleMessage.newBuilder().setId(echoConfig.getEchoId()).build();
    SimpleMessage resp;

    try {
      resp = blockingStub.echoBody(request);
    } catch (StatusRuntimeException ex) {
      LOGGER.error("RPC failed: {}", ex.getStatus());
      return;
    }

    LOGGER
        .info("Greeting, response message is: {}, cost time: {}. ", resp.toBuilder().buildPartial(),
            (System.currentTimeMillis() - start));
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
        .addObject(echoConfig)
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
        is = EchoClient.class.getClassLoader().getResourceAsStream(DEFAULT_LOGBACK_FILE);
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

@Parameters(separators = "=")
class EchoConfig extends AbstractConfig {

  @Parameter(names = {"--echo-id", "-echo-id"},
      description = "The param of invoke echo server.", required = true)
  private String echoId;

  @Parameter(names = {"--skylb-address", "-skylb-address"},
      description = "The address of skylb server.")
  private String skylbAddr = "skylb://localhost:1900/";

  public String getEchoId() {
    return echoId;
  }

  public String getSkylbAddr() {
    return skylbAddr;
  }

  @Override
  public String toKeyValues() {
    return new StringBuilder()
        .append("--echo-id").append("=").append(this.getEchoId()).append("\n")
        .append("--skylb-address").append("=").append(this.getSkylbAddr())
        .toString();
  }
}
