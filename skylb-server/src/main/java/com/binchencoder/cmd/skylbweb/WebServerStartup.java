package com.binchencoder.cmd.skylbweb;

import ch.qos.logback.core.joran.spi.JoranException;
import com.binchencoder.common.ShutdownHookThread;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerStartup {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebServerStartup.class);

  protected static SkyLbWebContext context;

  public WebServerStartup(String[] args)
      throws URISyntaxException, JoranException, UnknownHostException {
    this.context = this.createSkyLbWebContext(args);
  }

  public static void main(String[] args) {
    try {
      final WebServerStartup startup = new WebServerStartup(args);
      Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(LOGGER, (Callable<Void>) () -> {
        terminate();
        return null;
      }));

      startup.start();
      Thread.sleep(Long.MAX_VALUE);
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
    } finally {
      String tip = "The SkyLB Web Server boot successed. HTTP port="
          + context.getServerConfig().getHttpPort();
      LOGGER.info(tip);
      System.out.println(tip);
    }
  }

  private void start() throws Exception {
    try {
      context.start();
    } catch (Exception e) {
      this.context.terminate(e);
    } finally {
      this.terminate();
    }

    Exception error = this.context.getError();
    if (error != null) {
      throw error;
    }
  }

  private static void terminate() {
    Thread terminationThread = context.terminate();
    if (terminationThread != null) {
      try {
        terminationThread.join();
      } catch (InterruptedException e) {
        // ignore error
      }
    }
  }

  private static SkyLbWebContext createSkyLbWebContext(String[] args)
      throws JoranException, UnknownHostException, URISyntaxException, StatusRuntimeException {
    // Parsing the commander the parameters
    final SkyLbWebContext skyLbWebContext = new SkyLbWebContext(args);
    return skyLbWebContext;
  }
}
