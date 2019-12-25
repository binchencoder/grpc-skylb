package com.binchencoder.skylbweb.monitoring;

import static com.binchencoder.skylbweb.monitoring.SkyLbMetrics.reportingTypeHttp;

import com.binchencoder.cmd.skylbweb.SkyLbWebContext;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.monitoring.SkyLbHealthCheck;
import com.binchencoder.skylb.svclist.SvcListServlet;
import com.binchencoder.skylbweb.config.MetricsConfig;
import com.binchencoder.util.StoppableTask;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import com.google.common.base.Strings;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbWebHTTPServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbWebHTTPServer.class);

  public static void startIfRequired(SkyLbWebContext context) throws IOException {
    MetricsConfig metricsConfig = context.getMetricsConfig();
    if (Strings.isNullOrEmpty(metricsConfig.getMetricsType())) {
      LOGGER.warn("Metrics will not be exposed: metricsReportingType not configured.");
    }

    SkyLbMetrics.Registries metricsRegistries = getMetricsRegistries(context);
    if (null != metricsRegistries) {
      LOGGER.info("SkyLB web metrics http server starting");
    }

    int httpPort = context.getServerConfig().getHttpPort();
    InetAddress httpBindAddress = metricsConfig.getHttpBindAddress();
    String pathPrefix = metricsConfig.getHttpPathPrefix();
    SkyLblWebHTTPServerWorker skyLblHTTPServerWorker = new SkyLblWebHTTPServerWorker(
        httpBindAddress, httpPort, pathPrefix, metricsRegistries, context.getEtcdClient());

    Thread thread = new Thread(skyLblHTTPServerWorker);

    context.addTask(skyLblHTTPServerWorker);
    thread.setUncaughtExceptionHandler((t, e) -> {
      LOGGER.error("SkyLB metrics http server failure", e);
      context.terminate((Exception) e);
    });

    thread.setDaemon(true);
    thread.run();
    LOGGER.info("SkyLB metrics http server started on host:port => {}:{} ",
        httpBindAddress.getHostAddress(), httpPort);
  }

  private static SkyLbMetrics.Registries getMetricsRegistries(SkyLbWebContext context)
      throws IOException {
    MetricsConfig metricsConfig = context.getMetricsConfig();
    String reportingType = metricsConfig.getMetricsType();
    if (reportingType != null && reportingType.contains(reportingTypeHttp)) {
      metricsConfig.healthCheckRegistry.register("SkyLbHealth", new SkyLbHealthCheck(null));
      return new SkyLbMetrics.Registries(metricsConfig.metricRegistry,
          metricsConfig.healthCheckRegistry);
    } else {
      return null;
    }
  }
}

class SkyLblWebHTTPServerWorker implements StoppableTask, Runnable {

  private final InetAddress bindAddress;
  private int port;
  private final String pathPrefix;
  private final SkyLbMetrics.Registries metricsRegistries;

  private final EtcdClient etcdClient;

  private Server server;

  public SkyLblWebHTTPServerWorker(InetAddress bindAddress, int port, String pathPrefix,
      SkyLbMetrics.Registries metricsRegistries, EtcdClient etcdClient) {
    this.bindAddress = bindAddress;
    this.port = port;
    this.pathPrefix = pathPrefix;
    this.metricsRegistries = metricsRegistries;
    this.etcdClient = etcdClient;
  }

  public void startServer() throws Exception {
    if (this.bindAddress != null) {
      this.server = new Server(new InetSocketAddress(this.bindAddress, port));
    } else {
      this.server = new Server(this.port);
    }
    ServletContextHandler handler = new ServletContextHandler(this.server, pathPrefix);
    handler.addServlet(new ServletHolder(new SvcListServlet(etcdClient)), "/svc/list");

    if (metricsRegistries != null) {
      // TODO: there is a way to wire these up automagically via the AdminServlet, but it escapes me right now
      handler.addServlet(new ServletHolder(new MetricsServlet(metricsRegistries.metricRegistry)),
          "/metrics");
      handler.addServlet(new ServletHolder(new io.prometheus.client.exporter.MetricsServlet()),
          "/monitoring");
      handler.addServlet(
          new ServletHolder(new HealthCheckServlet(metricsRegistries.healthCheckRegistry)),
          "/healthcheck");
      handler.addServlet(new ServletHolder(new PingServlet()), "/ping");
    }

    this.server.start();
    this.server.join();
  }

  @Override
  public void run() {
    try {
      startServer();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void requestStop() throws Exception {
    if (null != this.server) {
      this.server.stop();
    }
  }

  @Override
  public void awaitStop(Long timeout) throws TimeoutException {
  }
}
