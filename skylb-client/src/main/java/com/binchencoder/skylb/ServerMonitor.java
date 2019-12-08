package com.binchencoder.skylb;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ServerMonitor 用来监控服务器列表的变化。
 */
public class ServerMonitor {

  private static final Logger logger = LoggerFactory.getLogger(ServerMonitor.class);

  private final CopyOnWriteArrayList<InetSocketAddress> skylbAddresses =
      new CopyOnWriteArrayList<InetSocketAddress>();
  private final AtomicInteger curIndex = new AtomicInteger(0);
  private ServerDiscovery discovery;
  private ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture<?> sFuture;
  private ScheduledFuture<?> debugPrintFuture;
  private Random random = new Random();

  private String callerServiceName;

  /**
   * @param skylbAddrs Skylb 服务地址列表。
   */
  public ServerMonitor(final List<InetSocketAddress> skylbAddrs) {
    skylbAddresses.addAll(skylbAddrs);
    if (skylbAddresses.isEmpty()) {
      throw new IllegalArgumentException("Empty skylb addresses");
    }

    curIndex.set(this.random.nextInt(skylbAddresses.size()));
    refreshNextServerDiscovery();
  }

  public void setCallerServiceName(String callerServiceName) {
    this.callerServiceName = callerServiceName;
  }

  public void shutdown() {
    logger.debug("Shutting down the server monitor.");
    if (this.sFuture != null && !this.sFuture.isDone()) {
      this.sFuture.cancel(true);
    }

    if (this.debugPrintFuture != null && !this.debugPrintFuture.isDone()) {
      this.debugPrintFuture.cancel(true);
    }

    if (this.scheduledExecutorService != null
        && !this.scheduledExecutorService.isShutdown()) {
      this.scheduledExecutorService.shutdownNow();
    }

    discovery.shutdown();
  }

  private synchronized void refreshNextServerDiscovery() {
    logger.info("Refresh next skylb server discovery.");

    if (discovery != null) {
      discovery.shutdown();
      logger.info("Shutting down discovery for skylb server {}.",
          discovery.getAddress());
    }

    InetSocketAddress addr = skylbAddresses.get(
        curIndex.getAndIncrement() % skylbAddresses.size());
    discovery = new ServerDiscovery(addr);
    logger.info("Created new discovery for skylb server {}.", addr);
  }

  /**
   * start 启动对 serviceName 的服务监听.
   */
  public void start(final ServiceSpec serviceSpec, final ServerListener listener)
      throws Exception {
    if (listener == null) {
      throw new Exception("Listener can not be null.");
    }

    this.scheduledExecutorService = Executors.newScheduledThreadPool(2,
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ServerMonitor-%d").build());

    this.sFuture = this.scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        logger.debug("Start monitor for skylb server {} for service {}.",
            discovery.getAddress(), serviceSpec.getServiceName());

        try {
          discovery.refreshLoop(callerServiceName, serviceSpec, new ServerListener() {
            @Override
            public void onChange(ServiceEndpoints endpoints) {
              logger.debug("Received endpoints {} for gRPC service {} from skylb server {}",
                  SkyLBUtils.endpointsToString(endpoints), serviceSpec.getServiceName(),
                  discovery.getAddress());

              if (endpoints.getSpec() == null) {
                logger.warn("Invalid endpoints: no spec.");
                return;
              }
              if (endpoints.getInstEndpointsCount() == 0) {
                logger.warn("Service endpoints is empty for service: {}",
                    endpoints.getSpec().getServiceName());
              }
              listener.onChange(endpoints);
              logger.info("Succeeded to refresh from skylb server {}\n{}", discovery.getAddress(),
                  SkyLBUtils.endpointsToString(endpoints));
            }

            @Override
            public String serverInfoToString() {
              return "NA"; // Not used
            }
          });
        } catch (Exception e) {
          logger.warn("Failed to refresh from skylb server {} for service {}. {}",
              discovery.getAddress(), serviceSpec.getServiceName(), e.getMessage());

          String msg = e.getMessage();
          if (msg != null && msg.contains("unexpected end of JSON input")) {
            logger.warn(
                "Unexpected end of JSON input, discard skylb server {} monitor for service {}.",
                discovery.getAddress(), serviceSpec.getServiceName());
            return;
          }
        }

        // Always recreate the discovery so that the connections landing on
        // SkyLB servers could balance automatically.
        refreshNextServerDiscovery();
      }
    }, 0, 3, TimeUnit.SECONDS);

    logger.debug("Debug print with interval {} sec", Properties.debugSvcInterval);
    this.debugPrintFuture = this.scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        if (logger.isDebugEnabled()) {
          logger.debug("Current service endpoints: {}", listener.serverInfoToString());
        }
      }
    }, 0, Properties.debugSvcInterval, TimeUnit.SECONDS);
  }
}
