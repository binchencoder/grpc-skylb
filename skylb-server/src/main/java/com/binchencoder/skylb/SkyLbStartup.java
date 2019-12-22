package com.binchencoder.skylb;

import ch.qos.logback.core.joran.spi.JoranException;
import com.binchencoder.common.ShutdownHookThread;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbStartup {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbStartup.class);

  private static SkyLbContext skyLbContext;

  public static void main(String[] args) {
    try {
      skyLbContext = createSkyLbContext(args);
      String tip =
          "The SkyLB Server boot successed. gRPC port=" + skyLbContext.getServerConfig().getPort();
      LOGGER.info(tip);
//      System.out.printf("%s%n", tip);

      Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(LOGGER, (Callable<Void>) () -> {
        terminate();
        return null;
      }));

      skyLbContext.start();
    } catch (JoranException e) {
      // catch JoranException explicitly because we likely don't care about the stacktrace
      LOGGER.error("JoranException: " + e.getLocalizedMessage());
      System.exit(1);
    } catch (IOException e) {
      LOGGER.error("IOException: " + e.getLocalizedMessage());
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void terminate() {
    Thread terminationThread = skyLbContext.terminate();
    if (terminationThread != null) {
      try {
        terminationThread.join();
      } catch (InterruptedException e) {
        // ignore error
      }
    }
  }

  private static SkyLbContext createSkyLbContext(String[] args) throws JoranException {
    // Parsing the commander the parameters
    final SkyLbContext skyLbContext = new SkyLbContext(args);
    return skyLbContext;
  }
}
