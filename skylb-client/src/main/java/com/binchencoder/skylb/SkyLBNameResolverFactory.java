package com.binchencoder.skylb;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.binchencoder.skylb.balancer.consistenthash.ConsistentHashLoadBalancerFactory;
import com.binchencoder.skylb.balancer.roundrobin.RoundRobinLoadBalancerFactory;
import com.binchencoder.skylb.metrics.Configuration;
import com.binchencoder.skylb.metrics.Constants;
import com.binchencoder.skylb.proto.ClientProtos.InstanceEndpoint;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancer.Helper;
import io.grpc.LoadBalancerProvider;
import io.grpc.LoadBalancerRegistry;
import io.grpc.NameResolver;
import io.grpc.NameResolver.Listener;
import io.grpc.Status;
import io.prometheus.client.Gauge;

public class SkyLBNameResolverFactory extends NameResolver.Factory {
  private static final Logger logger = LoggerFactory.getLogger(SkyLBNameResolverFactory.class);

  private static final Gauge.Builder svcEndpointBuilder = Gauge.build()
      .namespace("skylb")
      .subsystem("client")
      .name("service_endpoint_gauge")
      .labelNames(Constants.GRPC_SERVICE)
      .help("Number of service endpoints.");

  private static Gauge svcEndpointGauge = svcEndpointBuilder.register(Configuration.allMetrics().getCollectorRegistry());

  private ConcurrentHashMap<Spec, NameResolver> resolverMap =
      new ConcurrentHashMap<Spec, NameResolver>();

  private ServerMonitor monitor;
  private String monitorAddress;

  /**
   * For service graph tracking.
   */
  private String callerServiceName;

  private SkyLBNameResolverFactory() {
  }

  // register jg-round-robin and jg-consisten-hash load balancer to LoadBalancerRegistry.
  static {
    LoadBalancerRegistry.getDefaultRegistry().register(
        new LoadBalancerProvider() {

          @Override
          public boolean isAvailable() {
            return true;
          }

          @Override
          public int getPriority() {
            return 0;
          }

          @Override
          public String getPolicyName() {
            return SkyLBConst.JG_ROUND_ROBIN;
          }

          @Override
          public LoadBalancer newLoadBalancer(Helper helper) {
            return RoundRobinLoadBalancerFactory.getInstance().newLoadBalancer(helper);
          }

        });

    LoadBalancerRegistry.getDefaultRegistry().register(
        new LoadBalancerProvider() {

          @Override
          public boolean isAvailable() {
            return true;
          }

          @Override
          public int getPriority() {
            return 0;
          }

          @Override
          public String getPolicyName() {
            return SkyLBConst.JG_CONSISTEN_HASH;
          }

          @Override
          public LoadBalancer newLoadBalancer(Helper helper) {
            return ConsistentHashLoadBalancerFactory.getInstance().newLoadBalancer(helper);
          }

        });
  }

  public static synchronized SkyLBNameResolverFactory getInstance(String callerServiceName) {
    SkyLBUtils.checkServiceName(callerServiceName);
    SkyLBNameResolverFactory factory = new SkyLBNameResolverFactory();
    factory.callerServiceName = callerServiceName;
    return factory;
  }

  public final synchronized void start(URI targetUri, Listener listener) {
    NameResolver r = newNameResolver(targetUri, Attributes.newBuilder().build());
    r.start(listener);
  }
  /**
   * Creates a {@link NameResolver} for the given target URI.
   */
  @Override
  public NameResolver newNameResolver(URI targetUri, Attributes params) {
    logger.debug("newNameResolver, targetUri:{}", targetUri);
    if (!SkyLBConst.SKYLB_SCHEME.equals(targetUri.getScheme())) {
      logger.error("Invalid uri scheme {}", targetUri.getScheme());
      return null;
    }

    synchronized (this) {
      if (monitor == null) {
        String authority = Preconditions.checkNotNull(targetUri.getAuthority(), "authority");
        List<InetSocketAddress> addrs = SkyLBUtils.parseAddress(authority, SkyLBConst.SKYLB_PORT);
        Collections.shuffle(addrs);
        try {
          monitor = new ServerMonitor(addrs);
        } catch (IllegalArgumentException ex) {
          logger.error("targetUri {}, authority {}", targetUri, authority, ex);
          throw ex;
        }
        monitor.setCallerServiceName(this.callerServiceName);
        monitorAddress = authority;
      } else {
        String authority = Preconditions.checkNotNull(targetUri.getAuthority(), "authority");
        if (!authority.equals(monitorAddress)) {
          String errMsg = new StringBuilder("skylb address ")
              .append(authority)
              .append(" is ignored for it's not the same as before: ")
              .append(monitorAddress)
              .toString();
          // Log it and then throw it out.
          logger.error(errMsg);
          throw new RuntimeException(errMsg);
        }
      }
    }

    Spec spec = specFromURI(targetUri);

    synchronized (resolverMap) {
      NameResolver r = resolverMap.get(spec);
      if (r != null) {
        return r;
      }

      r = new SkyLBNameResolver(spec);
      resolverMap.put(spec, r);
      return r;
    }
  }

  @Override
  public String getDefaultScheme() {
    return SkyLBConst.SKYLB_SCHEME;
  }

  class SkyLBNameResolver extends NameResolver {
    private Spec spec;
    private Listener listener;

    private SkyLBNameResolver(Spec spec) {
      this.spec = spec;
    }

    @Override
    public String getServiceAuthority() {
      return this.spec.getServiceName();
    }

    @Override
    public final synchronized void start(Listener listener) {
      logger.debug("start");
      if (null != this.listener) {
        logger.info("ATTENTION: Reinit");
        // Now it's OK to init multiple times. Here prints for record.
      }
      if (null == listener) {
        logger.error("listener is null");
        System.exit(3);
      }
      this.listener = listener;

      resolve();
    }

    private void resolve() {
      final Spec listenedSpec = this.spec;

      try {
        monitor.start(this.spec.toServiceSpec(), new ServerListener() {
          private List<ResolvedServerInfoGroup> latestServers =
              new CopyOnWriteArrayList<>();

          @Override
          public void onChange(ServiceEndpoints endpoints) {
            if (endpoints.getSpec() == null) {
              return;
            }

            logger.debug("onChange spec:{}, listened:{}", endpoints.getSpec(), listenedSpec);
            Spec receivedSpec = new Spec(endpoints.getSpec().getServiceName(),
                endpoints.getSpec().getNamespace(),
                endpoints.getSpec().getPortName());

            if (!Objects.equals(receivedSpec, listenedSpec)) {
              logger.debug("{} be ignored", receivedSpec);
              return;
            }

            // Now we check the full endpoint list against local cache.
            boolean changed = false;
            // Convert List<List> to List for easier iterating.
            List<ResolvedServerInfoGroup> snapshot = new LinkedList<>();
            for (ResolvedServerInfoGroup servers : this.latestServers) {
              if (servers.getResolvedServerInfoList().isEmpty()) {
                continue;
              }
              snapshot.add(servers);
            }

            List<ResolvedServerInfo> gotSis = new ArrayList<>();
            for (InstanceEndpoint endpoint : endpoints.getInstEndpointsList()) {
              ResolvedServerInfo serverInfo = fromEndpoint(endpoint);
              gotSis.add(serverInfo);
            }

            // If there's any difference at all, overwrite local with the new list.
            if (snapshot.size() != gotSis.size()) {
              changed = true;
            } else {
              // Same size, see if elements are the same.
              findSame:
              for (ResolvedServerInfo serverInfo : gotSis) {
                for (ResolvedServerInfoGroup si : snapshot) {
                  if (equalsResolvedServerInfo120(si, serverInfo)) {
                    continue findSame;
                  }
                }
                changed = true;
                break;
              }
            }

            if (changed) {
              this.latestServers.clear();
              for (ResolvedServerInfo serverInfo : gotSis) {
                ResolvedServerInfoGroup toAddServices = ResolvedServerInfoGroup.builder().add(serverInfo).build();
                this.latestServers.add(toAddServices);
              }
            }

            if (changed) {
              // convert ResolvedServerInfoGroup to EquivalentAddressGroup
              List<EquivalentAddressGroup> servers = new ArrayList<>();
              for (ResolvedServerInfoGroup rsig: this.latestServers) {
                servers.add(rsig.toEquivalentAddressGroup());
              }
              listener.onAddresses(servers, Attributes.EMPTY);
              logger.info("Update servers for '{}', {}", listenedSpec.getServiceName(),
                  SkyLBUtils.serverInfoToString(this.latestServers));
              svcEndpointGauge.labels(listenedSpec.getServiceName()).set(this.latestServers.size());
            }
          } // end of onChange

          @Override
          public String serverInfoToString() {
            return SkyLBUtils.serverInfoToString(this.latestServers);
          }
        });
      } catch (Exception e) {
        listener.onError(Status.UNAVAILABLE.withCause(e));
        logger.warn("Resolve '{}' error {}", listenedSpec.getServiceName(), e);
        return;
      }
    }

    @Override
    public void shutdown() {
      logger.debug("shutdown");
      monitor.shutdown();
    }

    /**
     * fromEndpoint changes create a ResolvedServerInfo from Endpoint.
     */
    private ResolvedServerInfo fromEndpoint(InstanceEndpoint endpoint) {
      ResolvedServerInfo serverInfo = new ResolvedServerInfo(
          new InetSocketAddress(endpoint.getHost(), endpoint.getPort()),
          Attributes.newBuilder()
          .set(Attributes.Key.of(SkyLBConst.ENDPOINT_OP), endpoint.getOp())
          .set(Attributes.Key.of(SkyLBConst.ENDPOINT_WEIGHT), endpoint.getWeight())
          .build());

      return serverInfo;
    }

    /**
     * equalsResolvedServerInfo checks if two ResolvedServerInfo objects are
     * equal: with same address, same port.
     *
     * @param si1 (this one should not be null)
     * @return true if si1 and si2 are equal.
     */
    boolean equalsResolvedServerInfo(ResolvedServerInfo si1, ResolvedServerInfo si2) {
      InetSocketAddress a = (InetSocketAddress) si1.getAddress();
      if (null == si1 || null == a.getAddress()) {
        logger.warn("First ResolvedServerInfo should not be null");
        return false;
      }
      InetSocketAddress b = (InetSocketAddress) si2.getAddress();
      return a.getAddress().equals(b.getAddress()) && a.getPort() == b.getPort();
    }

    /**
     * equalsResolvedServerInfo120 is like equalsResolvedServerInfo, but works
     * for grpc 1.2.0.
     */
    boolean equalsResolvedServerInfo120(ResolvedServerInfoGroup si1, ResolvedServerInfo si2) {
      if (null == si1 || null == si1.getResolvedServerInfoList()
          || si1.getResolvedServerInfoList().isEmpty()) {
        return false;
      }
      return equalsResolvedServerInfo(si1.getResolvedServerInfoList().get(0), si2);
    }
  }

  private Spec specFromURI(URI targetUri) {
    String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
    Preconditions.checkArgument(targetPath.startsWith("/"),
        "the path component (%s) of the target (%s) must start with '/'", targetPath, targetUri);
    String serviceName = targetPath.substring(1);

    Map<String, String> paramMap = SkyLBUtils.parseQuery(targetUri);
    String portName = Preconditions.checkNotNull(
        paramMap.get(SkyLBConst.URI_QUERY_PORTNAME), "port name");

    String namespace = paramMap.get(SkyLBConst.URI_QUERY_NAMESPACE);
    if (namespace == null) {
      namespace = SkyLBConst.defaultNameSpace;
    }

    return new Spec(serviceName, namespace, portName);
  }
}
