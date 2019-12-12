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
import com.binchencoder.skylb.proto.SkylbGrpc.SkylbImplBase;
import com.binchencoder.skylb.utils.GrpcContextUtils;
import io.grpc.stub.StreamObserver;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
    super.resolve(request, responseObserver);

    SocketAddress remoteAddr = GrpcContextUtils.getRemoteAddr();
    LOGGER.info("Remote ip: {}", remoteAddr.toString());
  }

  @Override
  public StreamObserver<ReportLoadRequest> reportLoad(
      StreamObserver<ReportLoadResponse> responseObserver) {
    String remoteIP = GrpcContextUtils.getRemoteIP();
    LOGGER.info("Remote ip: {}", remoteIP);

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
