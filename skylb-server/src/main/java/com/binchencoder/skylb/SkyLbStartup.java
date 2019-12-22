package com.binchencoder.skylb;

import ch.qos.logback.core.joran.spi.JoranException;
import com.binchencoder.common.ShutdownHookThread;
import java.io.IOException;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbStartup {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbStartup.class);

  public static void main(String[] args) {
    try {
      SkyLbContext skyLbContext = createSkyLbContext(args);
      skyLbContext.start();

      String tip =
          "The SkyLB Server boot successed. gRPC port=" + skyLbContext.getServerConfig().getPort();
      LOGGER.info(tip);
      System.out.printf("%s%n", tip);

      Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(LOGGER, (Callable<Void>) () -> {
        skyLbContext.terminate();
        return null;
      }));
    } catch (JoranException e) {
      // catch JoranException explicitly because we likely don't care about the stacktrace
      LOGGER.error("JoranException: " + e.getLocalizedMessage());
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }

  }

  private static SkyLbContext createSkyLbContext(String[] args) throws JoranException {
    // Parsing the commander the parameters
    final SkyLbContext skyLbContext = new SkyLbContext(args);
    return skyLbContext;
  }


}
