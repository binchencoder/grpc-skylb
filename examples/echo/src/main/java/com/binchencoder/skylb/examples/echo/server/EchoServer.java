package com.binchencoder.skylb.examples.echo.server;

import com.binchencoder.easegw.examples.EchoServiceGrpc;
import com.binchencoder.easegw.examples.ExamplesProto.SimpleMessage;
import com.binchencoder.grpc.GrpcService;
import com.binchencoder.grpc.configurations.MetricsConfig;
import com.binchencoder.grpc.errors.Errors.ErrorCode;
import com.binchencoder.grpc.exceptions.BadRequestException;
import com.binchencoder.grpc.interceptors.ExceptionInterceptor;
import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, JmsAutoConfiguration.class,
    ActiveMQAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@ComponentScan(basePackages = {"com.binchencoder"})
public class EchoServer implements JettyServerCustomizer {

  private static final Logger logger = LoggerFactory.getLogger(EchoServer.class);

  @Autowired
  private MetricsConfig metricsConfig;

  public static void main(String[] args) {
    SpringApplication.run(EchoServer.class, args);

    logger.info("Binchencoder skylb examples echo Server is running!");
  }

  @Override
  public void customize(Server server) {
    ServerConnector connector = (ServerConnector) server.getConnectors()[0];
    connector.setPort(metricsConfig.getScrapePort());

    ServletContextHandler handler = (ServletContextHandler) server.getHandler();
    handler.addServlet(new ServletHolder(new MetricsServlet()), "/_/metrics");
  }
}
