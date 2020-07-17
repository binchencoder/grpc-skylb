# grpc-skylb

gRPC SkyLB: an external gRPC load balancer based on SkyDNS. Implemented in the Java language. The design fllows the gRPC Load Balancer Architecture (https://github.com/grpc/grpc/blob/master/doc/load-balancing.md)

## Overview

Different from traditional RPC systerm. gRPC is based on HTTP2 thus the recommended way to call an RPC is to create one long lived connect and use it for all clients (multiplexing and streaming).

It has the benefit of very high efficiency and throughput. However, it gives the traditional load balance architecture a difficulty to fit in. These architectures works nicely for ephemeral connections, but failed badly in gRPC.