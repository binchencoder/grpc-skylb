package com.binchencoder.skylb;

import com.beust.jcommander.internal.Lists;
import com.binchencoder.skylb.config.EtcdConfig;
import com.binchencoder.skylb.config.ServerConfig;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.grpc.SkyLbServiceImpl;
import com.binchencoder.skylb.hub.EndpointsHub;
import com.binchencoder.skylb.hub.EndpointsHubImpl;
import com.binchencoder.skylb.interceptors.HeaderInterceptor;
import com.binchencoder.skylb.interceptors.HeaderServerInterceptor;
import io.grpc.BindableService;
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

  private final EtcdConfig etcdConfig;
  private final ServerConfig serverConfig;

  private EtcdClient etcdClient;
  private EndpointsHub endpointsHub;

  private Server server;

  public SkyLbController(EtcdConfig etcdConfig, ServerConfig serverConfig) {
    this.etcdConfig = etcdConfig;
    this.serverConfig = serverConfig;
  }

  public boolean initialize() {
    this.etcdClient = new EtcdClient(etcdConfig);
    this.endpointsHub = new EndpointsHubImpl(etcdClient, serverConfig);
    return true;
  }

  public void start() throws IOException {
    final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(serverConfig.getPort());

    BindableService srv = new SkyLbServiceImpl(etcdClient, endpointsHub);
    // Bind server interceptors
    serverBuilder.addService(this.bindInterceptors(srv.bindService()));

    this.server = serverBuilder.build().start();
    this.startDaemonAwaitThread();
  }

  public void shutdown() {
    LOGGER.info("Shutting down gRPC server ...");
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

  public EtcdConfig getEtcdConfig() {
    return etcdConfig;
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }
}
