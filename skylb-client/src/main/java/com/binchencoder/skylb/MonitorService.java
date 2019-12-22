package com.binchencoder.skylb;

import static spark.Spark.get;
import static spark.Spark.ipAddress;
import static spark.Spark.port;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.binchencoder.skylb.metrics.SimpleTextFormat;

import io.prometheus.client.CollectorRegistry;
import spark.Request;
import spark.Response;
import spark.Route;

public class MonitorService {
  private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
  private static final MonitorService instance = new MonitorService();
  private boolean started = false;

  public static MonitorService getInstance() {
    return instance;
  }
  private MonitorService(){}

  /**
   * 如果　skylb client　应用已经启动了自己的 Prometheus web server, 不用调用这个方法。
   * 如果　skylb client 应用没有启动自己的　Prometheus web server, 可以调用这个方法来启动一个.
   * @param ipaddr
   * @param port
   * @param path
   */
  public synchronized void startPrometheus(String ipaddr, int port, String path) {
    if (started) {
      logger.info("Prometheus Server already started on {}:{}{}", ipaddr, port, path);
      return;
    }

    Preconditions.checkArgument(ipaddr != null && !"".equals(ipaddr.trim()),
        "Ip address can not be empty!");
    Preconditions.checkArgument(port > 0, "port must be >0");
    Preconditions.checkArgument(path.startsWith("/"), "path must start with '/'");

    ipAddress(ipaddr);
    port(port);

    // monitoring metrics initialization.
    get(path, new Route() {
      @Override
      public Object handle(Request req, Response res) throws Exception {
        res.status(HttpServletResponse.SC_OK);
        res.type(SimpleTextFormat.CONTENT_TYPE_004);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        PrintWriter writer = new PrintWriter(baos);
        SimpleTextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
        writer.flush();
        writer.close();

        return baos.toString();
      }
    });

    started = true;
    logger.info("Prometheus Server started on {}:{}{}", ipaddr, port, path);
  }
}
