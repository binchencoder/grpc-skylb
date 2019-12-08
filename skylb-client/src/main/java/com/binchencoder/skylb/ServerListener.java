package com.binchencoder.skylb;

import com.binchencoder.skylb.proto.ClientProtos.ServiceEndpoints;

public interface ServerListener {
  void onChange(ServiceEndpoints endpoints);

  String serverInfoToString();
}
