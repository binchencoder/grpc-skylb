package com.binchencoder.skylb;

import ch.qos.logback.core.joran.spi.JoranException;
import com.binchencoder.common.ShutdownHookThread;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbStartup {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbStartup.class);

  private static SkyLbContext skyLbContext;

  public static void main(String[] args) {
    try {
      skyLbContext = createSkyLbContext(args);
      Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(LOGGER, (Callable<Void>) () -> {
        terminate();
        return null;
      }));
      skyLbContext.start();

//      String tip =
//          "The SkyLB Server boot successed. gRPC port=" + skyLbContext.getServerConfig().getPort();
//      LOGGER.info(tip);
//      System.out.println(tip);
    } catch (JoranException e) {
      // catch JoranException explicitly because we likely don't care about the stacktrace
      LOGGER.error("JoranException: " + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    } catch (UnknownHostException ue) {
      LOGGER.error("UnknownHostException: " + ue.getLocalizedMessage());
      ue.printStackTrace(System.err);
      System.exit(1);
    } catch (IOException e) {
      LOGGER.error("IOException: " + e.getLocalizedMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    } catch (URISyntaxException e) {
      // catch URISyntaxException explicitly as well to provide more information to the user
      LOGGER.error(
          "Syntax issue with URI, check for configured --etcd-endpoints options (see RFC 2396)");
      LOGGER.error("URISyntaxException: " + e.getLocalizedMessage());
      e.printStackTrace(System.err);
    } catch (StatusRuntimeException sre) {
      sre.printStackTrace(System.err);
      System.exit(1);
    } catch (Exception e) {
      e.printStackTrace(System.err);
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

  private static SkyLbContext createSkyLbContext(String[] args)
      throws JoranException, UnknownHostException, URISyntaxException, StatusRuntimeException {
    // Parsing the commander the parameters
    final SkyLbContext skyLbContext = new SkyLbContext(args);
    return skyLbContext;
  }
}
