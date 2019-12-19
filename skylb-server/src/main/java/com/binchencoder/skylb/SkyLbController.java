package com.binchencoder.skylb;

import com.beust.jcommander.internal.Lists;
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
import com.binchencoder.skylb.prefix.InitPrefix;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbController.class);

  private final ServerConfig serverConfig;

  private EtcdClient etcdClient;
  private EndpointsHub endpointsHub;
  private SkyLbGraph skyLbGraph;
  private LameDuck lameDuck;

  private Server server;

  public SkyLbController(ServerConfig serverConfig) {
    this.serverConfig = serverConfig;
  }

  public boolean initialize() {
    this.etcdClient = new EtcdClient();
    this.lameDuck = new LameDuck(etcdClient);
    this.endpointsHub = new EndpointsHubImpl(etcdClient, lameDuck, serverConfig);
    this.skyLbGraph = new SkyLbGraphImpl(etcdClient);

    return true;
  }

  public void start() throws IOException {
    SkyLbServiceImpl skyLbService = new SkyLbServiceImpl(endpointsHub, lameDuck, skyLbGraph);

    // Bind server interceptors
    final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(serverConfig.getPort());
    serverBuilder.addService(this.bindInterceptors(skyLbService.bindService()));

    this.server = serverBuilder.build().start();
    this.startDaemonAwaitThread();

    // Initializes ETCD keys.
    try {
      InitPrefix.newInstance(etcdClient);
    } catch (Exception e) {
      LOGGER.error("Init prefix etcd key error", e);
      this.shutdown();
    }
  }

  public void shutdown() {
    LOGGER.info("Shutting down gRPC server ...");

    try {
      this.endpointsHub.close();
      this.skyLbGraph.close();
    } catch (Exception e) {
      LOGGER.error("AutoCloseable the resource error", e);
    }

    Optional.ofNullable(server).ifPresent(Server::shutdown);
    LOGGER.info("gRPC server stopped.");
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread = new Thread(() -> {
      try {
        SkyLbController.this.server.awaitTermination();
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

  public EtcdClient getEtcdClient() {
    return etcdClient;
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }
}
