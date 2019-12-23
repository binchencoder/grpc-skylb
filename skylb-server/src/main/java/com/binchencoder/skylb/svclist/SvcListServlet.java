package com.binchencoder.skylb.svclist;

import com.beust.jcommander.internal.Lists;
import com.binchencoder.skylb.etcd.Endpoints;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SvcListServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(SvcListServlet.class);

  private static final long serialVersionUID = -8432996484889177321L;
  private static final String CONTENT_TYPE = "application/json";

  private transient ObjectMapper mapper;

  private final EtcdClient etcdClient;

  public SvcListServlet(EtcdClient etcdClient) {
    this.etcdClient = etcdClient;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    this.mapper = new ObjectMapper();
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType(CONTENT_TYPE);
    resp.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");

    List<Endpoints> svcList = Lists.newArrayList();
    svcList.add(Endpoints.newBuilder()
        .setName("ease-gateway")
        .setNamespace("default")
        .build());

    try (OutputStream output = resp.getOutputStream()) {
      getWriter(req).writeValue(output, svcList);
    }
  }

  private ObjectWriter getWriter(HttpServletRequest request) {
    final boolean prettyPrint = Boolean.parseBoolean(request.getParameter("pretty"));
    if (prettyPrint) {
      return mapper.writerWithDefaultPrettyPrinter();
    }
    return mapper.writer();
  }
}
