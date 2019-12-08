package com.binchencoder.skylb.grpc;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.binchencoder.skylb.Properties;
import com.binchencoder.skylb.SkyLBNameResolverFactory;
import com.binchencoder.skylb.balancer.consistenthash.ConsistentHashLoadBalancerFactory;
import com.binchencoder.skylb.balancer.roundrobin.RoundRobinLoadBalancerFactory;
import com.binchencoder.skylb.healthcheck.SizerLBFactory;
import com.binchencoder.skylb.metrics.Configuration;
import com.binchencoder.skylb.metrics.MetricsClientInterceptor;
import com.binchencoder.skylb.util.ServiceNameUtil;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.LoadBalancer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * ClientTemplate provides helper methods to create {@link Channels} for grpc clients.
 * <br/>
 * Beware: when you want to close the channel, make sure to call {@link Channels#shutdown()}.
 */
public class ClientTemplate {
  public static final String DEFAULT_PORT_NAME = "grpc";

  private static final Logger logger = LoggerFactory.getLogger(ClientTemplate.class);
  private static final HeaderClientInterceptor headerInterceptor = new HeaderClientInterceptor();
  private static final ConsistentHashInterceptor hashKeyInterceptor = new ConsistentHashInterceptor();

  /**
   * (This is more of an internal method)
   *
   * @param target            In the form of skylb://x.x.x.x:1900/svcName1?portName=grpc
   *                          or direct://shared-test-server-service:127.0.0.1:50001
   * @param callerServiceName (only format will be checked)
   * @param calleeServiceName (only format will be checked)
   */
  public static Channels createChannel(String target, String callerServiceName, String calleeServiceName) {
    return createChannel(target, callerServiceName, calleeServiceName,
        RoundRobinLoadBalancerFactory.getInstance());
  }

  /**
   * (This is more of an internal method, and is the root of all createChannel
   * methods)
   *
   * @param target            In the form of skylb://x.x.x.x:1900/svcName1?portName=grpc
   *                          or direct://shared-test-server-service:127.0.0.1:50001
   * @param callerServiceName (only format will be checked)
   * @param calleeServiceName (only format will be checked)
   */
  public static Channels createChannel(String target, String callerServiceName,
                                       String calleeServiceName, LoadBalancer.Factory lbFactory) {
    logger.info("target {}, callerServiceName {}", target, callerServiceName);

    SizerLBFactory sizerLbFactory = null;
    if (Properties.enableHealthCheck && lbFactory instanceof SizerLBFactory) {
      sizerLbFactory = ((SizerLBFactory) lbFactory).newInstance();
      lbFactory = (LoadBalancer.Factory) sizerLbFactory;
      logger.debug("Customized load balancer factory for health checking: {}", lbFactory);
    }

    ManagedChannel originChannel;
    ManagedChannelBuilder<?> builder;
    if (target.startsWith(ServiceAddrUtil.SKY_LB)) {
      builder = ManagedChannelBuilder.forTarget(target)
          .nameResolverFactory(SkyLBNameResolverFactory.getInstance(callerServiceName))
          .intercept(MetricsClientInterceptor.create(
              Configuration.allMetrics(),
              calleeServiceName, callerServiceName))
          .loadBalancerFactory(lbFactory)
          .usePlaintext(true);
    } else if (target.startsWith(ServiceAddrUtil.DIRECT)) {
      URI uri = URI.create(target);
      logger.debug("host {} port {}", uri.getHost(), uri.getPort());
      builder = ManagedChannelBuilder.forAddress(uri.getHost(), uri.getPort())
          .intercept(MetricsClientInterceptor.create(
              com.binchencoder.skylb.metrics.Configuration.allMetrics(),
              calleeServiceName, callerServiceName))
          .loadBalancerFactory(lbFactory)
          .usePlaintext(true);
    } else {
      throw new IllegalArgumentException("Illegal schema");
    }

    if (Properties.grpcIdleTimeoutInSec > 0) {
      builder = builder.idleTimeout(Properties.grpcIdleTimeoutInSec, TimeUnit.SECONDS);
    }

    if (Properties.maxInboundMessageSize > 0) {
      builder = builder.maxInboundMessageSize(Properties.maxInboundMessageSize);
    }

    originChannel = builder.build();

    Channel channel = ClientInterceptors.intercept(originChannel, headerInterceptor);

    if (lbFactory instanceof ConsistentHashLoadBalancerFactory) {
      channel = ClientInterceptors.intercept(channel, hashKeyInterceptor);
    }

    Channels channels = new Channels(originChannel, channel);
    if (null != sizerLbFactory) {
      sizerLbFactory.setSizerUser(channels);
    }
    channels.setCalleeServiceName(calleeServiceName);
    return channels;
  }

  // @formatter:off
  /**
   * 根据给定参数创建gRPC channel
   *
   * direct 子串构成一个map， key 是服务的名字， value 是这个服务对应的直连地址。 direct 方式只能在开发和测试环境中使用。
   *
   * 如果被调用的 calleeServiceName 在 direct 中存在，就优先使用 map 中对应的直连地址进行直连，没有负载均衡功能。
   * 如果不存在，就通过 skylb 获得服务地址列表，使用 grpc 的负载均衡功能。
   *
   * @param skylbUri          格式为: skylb://a.b.c.d:1900,a.b.c.e:1900;direct://svcName:x.x.x.x:2016,svcName2:y.y.y.y:2017
   *                          上述定义限定：<br/>
   *                          1. 字串中只能有1个skylb://,如果解析时遇到0个或者多个，抛出 IllegalArgumentException。<br/>
   *                          2. 字串中只能有0-1个direct://,如果解析时不符合，抛出 IllegalArgumentException。<br/>
   *                          3. 一个serviceName只对应一个 IP:port 对, 不支持对应多个。
   * @param calleeServiceName 被调用 service 的 service name, 使用 direct 方式调用的情况下可以为null。
   *                          Must be valid service id defined in vexillary-client.
   * @param calleePortName    被调用 service 的 port name，可以为null。
   * @param calleeNamespace   被调用 service 的 namespace，可以为null。
   * @param callerServiceName 调用者的 service name。
   *                          Must be valid service id defined in vexillary-client.
   * @return gRPC Channel
   */
  // @formatter:on
  public static Channels createChannel(String skylbUri, String calleeServiceName,
                                       String calleePortName, String calleeNamespace,
                                       String callerServiceName) {
    return createChannel(skylbUri, calleeServiceName, calleePortName, calleeNamespace,
        callerServiceName, RoundRobinLoadBalancerFactory.getInstance());
  }

  /**
   * @param skylbUri          格式为: skylb://a.b.c.d:1900,a.b.c.e:1900;direct://svcName:x.x.x.x:2016,svcName2:y.y.y.y:2017
   * @param calleeServiceName Must be valid service id defined in
   *                          vexillary-client.
   * @param callerServiceName Must be valid service id defined in
   *                          vexillary-client.
   */
  public static Channels createChannel(String skylbUri, String calleeServiceName,
                                       String calleePortName, String calleeNamespace,
                                       String callerServiceName,
                                       LoadBalancer.Factory lbFactory) {
    logger.info(
        "skylbUri {}, calleeServiceName {}, calleePortName {}, calleeNamespace {}, callerServiceName {}",
        new Object[]{skylbUri, calleeServiceName, calleePortName, calleeNamespace,
            callerServiceName});
    if (Strings.isNullOrEmpty(calleeServiceName)) {
      throw new IllegalArgumentException("Illegal callee service name");
    }
    ServiceNameUtil.toServiceId(calleeServiceName);
    ServiceNameUtil.toServiceId(callerServiceName);

    ServiceAddrUtil sa = ServiceAddrUtil.parse(skylbUri);

    // Direct call backend service.
    String directAddr = sa.getDirectMap().get(calleeServiceName);
    if (directAddr != null) {
      logger.info("Creating direct channel directAddr {}, callerServiceName {}", directAddr,
          callerServiceName);
      return createChannel(directAddr, callerServiceName, calleeServiceName, lbFactory);
    }

    if (Strings.isNullOrEmpty(calleePortName)) {
      calleePortName = DEFAULT_PORT_NAME;
    }

    if (StringUtils.isBlank(sa.getSkylbAddr())) {
      logger.error(
          "Skylb Address is blank: skylbUri {}, calleeServiceName {}, calleePortName {}, calleeNamespace {}, callerServiceName {}",
          new Object[]{skylbUri, calleeServiceName, calleePortName, calleeNamespace,
              callerServiceName});
      throw new IllegalArgumentException("Skylb Address is blank");
    }

    // Construct a target string with given parameters.
    String target = new StringBuilder(sa.getSkylbAddr()).append("/").append(calleeServiceName)
        .append("?portName=").append(calleePortName).append("&namespace=")
        .append(Strings.isNullOrEmpty(calleeNamespace) ? "default" : calleeNamespace).toString();

    logger.info("Creating skylb channel target {}, callerServiceName {}", target,
        callerServiceName);
    return createChannel(target, callerServiceName, calleeServiceName, lbFactory);
  }

  /**
   * @param target In the form of skylb://x.x.x.x:1900/svcName1?portName=grpc
   * @deprecated Use {@link #createChannel(String, String, String)}
   */
  public static Channels createChannel(String target, String callerServiceName) {
    return createChannel(target, callerServiceName, RoundRobinLoadBalancerFactory.getInstance());
  }

  /**
   * @deprecated Use {@link #createChannel(String, String, String,
   * LoadBalancer.Factory)}
   */
  public static Channels createChannel(String target, String callerServiceName,
                                       LoadBalancer.Factory lbFactory) {
    new Throwable("Deprecated").printStackTrace();
    return createChannel(target, callerServiceName, "FIXME", lbFactory);
  }
}
