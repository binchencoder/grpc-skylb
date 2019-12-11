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
import io.grpc.Context;
import io.grpc.stub.StreamObserver;

public class SkyLbServiceImpl extends SkylbImplBase {

  private final EtcdClient etcdClient;

  private final EndpointsHub endpointsHub;

  public SkyLbServiceImpl(final EtcdClient etcdClient, final EndpointsHub endpointsHub) {
    this.etcdClient = etcdClient;
    this.endpointsHub = endpointsHub;
  }

  @Override
  public void resolve(ResolveRequest request, StreamObserver<ResolveResponse> responseObserver) {
    super.resolve(request, responseObserver);

    Context ctx = Context.current();
  }

  @Override
  public StreamObserver<ReportLoadRequest> reportLoad(
      StreamObserver<ReportLoadResponse> responseObserver) {
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
