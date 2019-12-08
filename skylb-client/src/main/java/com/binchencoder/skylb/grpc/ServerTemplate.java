package com.binchencoder.skylb.grpc;

import com.binchencoder.skylb.ReportCallback;
import com.binchencoder.skylb.SkyLBServiceReporter;
import com.binchencoder.skylb.grpchealth.JinHealthServiceImpl;
import com.binchencoder.skylb.grpchealth.JinHealthServiceInterceptor;
import com.binchencoder.skylb.metrics.Configuration;
import com.binchencoder.skylb.metrics.MetricsServerInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.BindableService;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;

public class ServerTemplate<T extends ServerBuilder<T>> {
  static Logger logger = LoggerFactory.getLogger(ServerTemplate.class);

  public static int DEFAULT_REPORT_INTERVAL = 3000; // 毫秒

  public static <T> ServerBuilder create(int port, BindableService bindableService,
                                         String serviceName, ServerInterceptor... interceptors) {
    // Record metrics.
    MetricsServerInterceptor metricsIncept = MetricsServerInterceptor.create(
        Configuration.allMetrics(), serviceName);
    // Intercept header for request id.
    HeaderServerInterceptor headerIncept = new HeaderServerInterceptor();

    ServerInterceptor[] ins = getServerInterceptors(metricsIncept, headerIncept, interceptors);

    return ServerBuilder.forPort(port)
        .addService(ServerInterceptors.intercept(bindableService, ins))
        // Enable grpc health checking.
        .addService(ServerInterceptors.intercept(new JinHealthServiceImpl(),
            new JinHealthServiceInterceptor(), metricsIncept, headerIncept));
  }

  public static <T> ServerBuilder create(int port, ServiceDefinition[] serviceDefs,
                                         ServerInterceptor... interceptors) {
    if (null == serviceDefs || 0 == serviceDefs.length) {
      throw new RuntimeException("No valid service definition");
    }

    MetricsServerInterceptor metricsIncept = MetricsServerInterceptor.create(
        Configuration.allMetrics(), "willBeReplaced");
    // Intercept header for request id.
    HeaderServerInterceptor headerIncept = new HeaderServerInterceptor();

    ServerInterceptor[] ins = getServerInterceptors(metricsIncept, headerIncept, interceptors);

    ServerBuilder<?> builder = ServerBuilder.forPort(port);

    for (ServiceDefinition def : serviceDefs) {
      metricsIncept = MetricsServerInterceptor.create(
          Configuration.allMetrics(), def.getServiceName());
      ins[0] = metricsIncept;
      builder = builder.addService(ServerInterceptors.intercept(def.getService(), ins));
    }

    // Enable grpc health checking.
    builder = builder.addService(ServerInterceptors.intercept(new JinHealthServiceImpl(),
        new JinHealthServiceInterceptor(), metricsIncept, headerIncept));
    return builder;
  }

  private static ServerInterceptor[] getServerInterceptors(MetricsServerInterceptor metricsIncept,
                                                           HeaderServerInterceptor headerIncept,
                                                           ServerInterceptor[] interceptors) {
    ServerInterceptor[] basicIns = new ServerInterceptor[]{
        metricsIncept, headerIncept
    };

    ServerInterceptor[] ins;
    if (null != interceptors && interceptors.length > 0) {
      ins = new ServerInterceptor[basicIns.length + interceptors.length];
      System.arraycopy(basicIns, 0, ins, 0, basicIns.length);
      System.arraycopy(interceptors, 0, ins, basicIns.length, interceptors.length);
    } else {
      ins = basicIns;
    }
    return ins;
  }

  /**
   * Report load to skylb server.
   * Should be applied to each service.
   */
  public static SkyLBServiceReporter reportLoad(String skylbUri, String serviceName, String portName, int port)
      throws Exception {
    // 1. create a reporter.
    SkyLBServiceReporter r = new SkyLBServiceReporter(skylbUri);
    // 2. (optional) set report interval.
    r.setReportInterval(DEFAULT_REPORT_INTERVAL);
    // 3. register
    r.register(serviceName, portName, port, new ReportCallback() {
      @Override
      public int getWeight() {
        return ReportCallback.DEFAULT_WEIGHT;
      }
    });
    return r;
  }

  /**
   * Report load to skylb server.
   * Should be applied to each service.
   * 
   * Instantiate ReportCallback to change weight.
   * new ReportCallback() {
   *   @Override
   *   public int getWeight() {
   *     return 3;
   *   }
   * }
   */
  public static SkyLBServiceReporter reportLoad(String skylbUri, String serviceName, String portName, int port, ReportCallback reportCallback)
      throws Exception {
    // 1. create a reporter.
    SkyLBServiceReporter r = new SkyLBServiceReporter(skylbUri);
    // 2. (optional) set report interval.
    r.setReportInterval(DEFAULT_REPORT_INTERVAL);
    // 3. register
    r.register(serviceName, portName, port, reportCallback);
    return r;
  }
}
