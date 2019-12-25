package com.binchencoder.skylbweb.svclist;

import static com.binchencoder.skylb.constants.EtcdConst.SEPARATOR;
import static com.binchencoder.skylb.prefix.InitPrefix.ENDPOINTS_KEY;
import static io.etcd.jetcd.options.GetOption.SortOrder.DESCEND;

import com.beust.jcommander.internal.Lists;
import com.binchencoder.skylb.etcd.Endpoints;
import com.binchencoder.skylb.etcd.Endpoints.EndpointPort;
import com.binchencoder.skylb.etcd.Endpoints.EndpointSubset;
import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.proto.ClientProtos.InstanceEndpoint;
import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.common.exception.ErrorCode;
import io.etcd.jetcd.common.exception.EtcdExceptionFactory;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simply lists all services.
 */
public class SvcListServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(SvcListServlet.class);

  private static final long serialVersionUID = -8432996484889177321L;
  private static final String CONTENT_TYPE = "application/json";

  private transient Printer printer;

  private final EtcdClient etcdClient;

  public SvcListServlet(EtcdClient etcdClient) {
    this.etcdClient = etcdClient;
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    this.printer = JsonFormat.printer();
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

    List<ServiceEndpoints> svcList = Lists.newArrayList();
    final ByteSequence bytesKey = ByteSequence.from(ENDPOINTS_KEY.getBytes());
    try {
      GetResponse etcdResp = etcdClient.getKvClient()
          .get(bytesKey, GetOption.newBuilder().withPrefix(bytesKey).withSortOrder(DESCEND).build())
          .get();
      for (KeyValue kv : etcdResp.getKvs()) {
        String v = kv.getValue().toString(Charset.defaultCharset());
        if (Strings.isNullOrEmpty(v)) {
          continue;
        }

        String k = kv.getKey().toString(Charset.defaultCharset());
        List<String> keyPaths = Lists.newArrayList(k.split(SEPARATOR));
        String hostPort = keyPaths.get(keyPaths.size() - 1);
        String[] ss = hostPort.split("_");
        if (ss.length < 2) {
          LOGGER.warn("Invalid hostPort: {}", hostPort);
          continue;
        }

        ServiceEndpoints.Builder endpointsBuilder = ServiceEndpoints.newBuilder()
            .addInstEndpoints(InstanceEndpoint.newBuilder()
                .setHost(ss[0])
                .setPort(Integer.parseInt(ss[1]))
                .build());
        ServiceSpec.Builder specBuilder = ServiceSpec.newBuilder()
            .setServiceName(keyPaths.get(keyPaths.size() - 2))
            .setNamespace(keyPaths.get(keyPaths.size() - 3));
        Endpoints eps = this.unmarshalEndpoints(v);
        if (eps.getSubsets().size() > 0) {
          List<EndpointSubset> subsets = Lists.newArrayList(eps.getSubsets());
          if (subsets.size() > 0) {
            List<EndpointPort> ports = Lists.newArrayList(subsets.get(0).getPorts());
            specBuilder.setPortName(ports.get(0).getName());
          }
        }
        endpointsBuilder.setSpec(specBuilder.build());
        svcList.add(endpointsBuilder.build());

        resp.getOutputStream()
            .println(printer.preservingProtoFieldNames().print(endpointsBuilder.buildPartial()));
      }
    } catch (Exception e) {
      LOGGER.error("Etcd get kvs with prefix [{}] error", ENDPOINTS_KEY, e);

      e.printStackTrace(resp.getWriter());
    }
  }

  private Endpoints unmarshalEndpoints(String v) {
    try {
      return new Gson().fromJson(v, Endpoints.class);
    } catch (JsonSyntaxException e) {
      LOGGER.error("unmarshal endpoints error, v: {}", v, e);
      throw EtcdExceptionFactory
          .newEtcdException(ErrorCode.INTERNAL, "unmarshal endpoints error: " + e.getMessage());
    }
  }
}
