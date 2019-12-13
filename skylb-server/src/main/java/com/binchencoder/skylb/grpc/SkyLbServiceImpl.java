package com.binchencoder.skylb.grpc;

import com.binchencoder.skylb.etcd.EtcdClient;
import com.binchencoder.skylb.hub.EndpointsHub;
import com.binchencoder.skylb.proto.ClientProtos.DiagnoseRequest;
import com.binchencoder.skylb.proto.ClientProtos.DiagnoseResponse;
import com.binchencoder.skylb.proto.ClientProtos.Operation;
import com.binchencoder.skylb.proto.ClientProtos.ReportLoadRequest;
import com.binchencoder.skylb.proto.ClientProtos.ReportLoadResponse;
import com.binchencoder.skylb.proto.ClientProtos.ResolveRequest;
import com.binchencoder.skylb.proto.ClientProtos.ResolveResponse;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.binchencoder.skylb.proto.SkylbGrpc.SkylbImplBase;
import com.binchencoder.skylb.utils.GrpcContextUtils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.net.InetSocketAddress;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyLbServiceImpl extends SkylbImplBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(SkyLbServiceImpl.class);

  private final EtcdClient etcdClient;

  private final EndpointsHub endpointsHub;

  public SkyLbServiceImpl(final EtcdClient etcdClient, final EndpointsHub endpointsHub) {
    this.etcdClient = etcdClient;
    this.endpointsHub = endpointsHub;
  }

  @Override
  public void resolve(ResolveRequest request, StreamObserver<ResolveResponse> responseObserver) {
    LOGGER.info("Caller service {}", request.getCallerServiceId());

    InetSocketAddress remoteAddr = (InetSocketAddress) GrpcContextUtils.getRemoteAddr();
    if (null == remoteAddr) {
      throw new StatusRuntimeException(
          Status.DATA_LOSS.withDescription("Failed to get peer client info from context."));
    }
    LOGGER.info("Remote ip: {}", remoteAddr.getHostString());

    List<ServiceSpec> specs = request.getServicesList();
    if (specs.isEmpty()) {
      throw new StatusRuntimeException(
          Status.INVALID_ARGUMENT.withDescription("No service spec found.."));
    }

    // TODO(chenbin) TrackServiceGraph
    for (ServiceSpec spec : specs) {
//      TrackServiceGraph
    }

    try {
      endpointsHub.addObserver(request.getServicesList(), remoteAddr.getHostName(),
          request.getResolveFullEndpoints());
    } catch (Exception e) {
      for (ServiceSpec spec : specs) {

      }

      throw e;
    }

    super.resolve(request, responseObserver);
  }

  @Override
  public StreamObserver<ReportLoadRequest> reportLoad(
      StreamObserver<ReportLoadResponse> responseObserver) {
    InetSocketAddress remoteAddr = (InetSocketAddress) GrpcContextUtils.getRemoteAddr();
    LOGGER.info("Remote ip: {}", remoteAddr.toString());

    return super.reportLoad(responseObserver);
  }

  @Override
  public StreamObserver<DiagnoseResponse> attachForDiagnosis(
      StreamObserver<DiagnoseRequest> responseObserver) {
    return super.attachForDiagnosis(responseObserver);
  }

  private String opToString(Operation op) {
    switch (op) {
      case Add:
        return "ADD";
      case Delete:
        return "DELETE";
      default:
        return "";
    }
  }
}
