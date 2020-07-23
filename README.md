gRPC SkyLB
==========

`gRPC SkyLB`: an external gRPC load balancer based on [External Load Balancing Service](https://github.com/grpc/grpc/blob/master/doc/load-balancing.md#external-load-balancing-service). Implemented in the Java language. The design follows the gRPC Load Balancer Architecture (https://github.com/grpc/grpc/blob/master/doc/load-balancing.md)

## Overview

Different from traditional RPC systerm. gRPC is based on HTTP2 thus the recommended way to call an RPC is to create one long lived connect and use it for all clients (multiplexing and streaming).

It has the benefit of very high efficiency and throughput. However, it gives the traditional load balance architecture a difficulty to fit in. These architectures works nicely for ephemeral connections, but failed badly in gRPC.

To fix this issue, a load balancing architecture proposal was created by the gRPC dev team in Febrary, 2016 and reached its maturity around July, 2016. We are copied [load-balancing.md](https://github.com/grpc/grpc/blob/master/doc/load-balancing.md) and cached the proposal. The core module in this proposal is an [external load balancer](https://github.com/grpc/grpc/blob/master/doc/load-balancing.md#external-load-balancing-service) which aims at both flexibility and the simplicity for the gRPC client programming.

However, there is not an official implementation for the external load balancer (and probably there will not be an official one since every company has its distinct production environment). This is the reason we initiated the project `gRPC-SkyLB`.

## Architecture of SkyLB

The design follows the gRPC "Load Balancer Architecture" [workflow](https://github.com/grpc/grpc/blob/master/doc/load-balancing.md#workflow)

![](./docs/images/architecture.png)

As shown in the diagram, we will go into the implementation details below:

- All service info is saved in an etcd cluster
- One or more `SkyLB` servers talk to the etcd cluster
- `SkyLB` service is highly available (more than one instances). Its load is rather light and stable
- At the first step, both the gRPC clients and services ask `SkyLB` to resolve the location of `SkyLB` instances. One `SkyLB` instance will be randomly chosen
- Target instance perspective
  - After a `SkyLB` instance is chosen, the target service sends a gRPC call to `SkyLB` with a *streaming request*
  - The request is treated as a heartbeat or load report. With a fixed interval, the target service keeps sending requests to `SkyLB`
  - `SkyLB` finds the remote address of the target instance, and saves it with other info (priority and weight) to etcd with a TTL
- Client instance perspective
  - After a `SkyLB` instance is chosen, the gRPC client sends a gRPC call (with the target service name) to `SkyLB`, and receives a *streaming response*
  - `SkyLB` should return a response right after the request arrives, and whenever the service changes
  - The response contains all services currently alive by the knowledge of `SkyLB`
  - The client establishes a gRPC connection to all target instances. When the client is notified for service change, it update the connections
  - For each gRPC call, the client instance chooses one connection to send requests. We can use the built-in `RoundRobin` or `ConsistentHash` policy to choose connection. In future we might have more complex load balancing policies

## Diagnostic Protocol

After a client connects to the `SkyLB` server, it starts a two-way gRPC stream call. The client then comes into a passive mode, waiting for instruction from the server. When the time comes (for example when user triggers it through UI), the server sends a specific request to the client, and the client executes the instruction and returns with a corresponding response.

> *NOTE*
>
> that whenever an error is caused during the streaming talk, the stream has to be discarded and a new stream should be created (by the client).

The API protocol is defined in [github.com/binchencoder/grpc-skylb/skylb-proto/api.proto](https://github.com/binchencoder/grpc-skylb/tree/master/skylb-proto).

## Usage

1. Dependency

   **Install skylb-client local repository**

   ```shell
   chenbindeMacBook-Pro:grpc-skylb chenbin$ mvn clean install
   ```

   ```java
   <dependency>
   	<groupId>com.binchencoder.skylb</groupId>
   	<artifactId>skylb-client</artifactId>
   	<version>1.0-SNAPSHOT</version>
   </dependency>
   ```

2. Implements gRPC API

   ```java
   public class DemoGrpcImpl extends DemoGrpc.DemoImplBase {
     private final Logger LOGGER = LoggerFactory.getLogger(DemoGrpcImpl.class);
   
     private Random rand = new Random(System.currentTimeMillis());
     private int port;
     public DemoGrpcImpl(int port_) {
       this.port = port_;
     }
   
     @Override
     public void greeting(GreetingProtos.GreetingRequest request,
         StreamObserver<GreetingProtos.GreetingResponse> responseObserver) {
       LOGGER.info("Got req: {}", request);
   
       // 随机耗时350~550毫秒.
       int elapse = 350 + rand.nextInt(200);
       try {
         TimeUnit.MILLISECONDS.sleep(elapse);
       } catch (InterruptedException ie) {
         LOGGER.warn("sleep interrupted");
       }
   
       GreetingResponse reply = GreetingResponse.newBuilder().setGreeting(
           "Hello " + request.getName() + ", from :" + port + ", elapse " + elapse + "ms").build();
       responseObserver.onNext(reply);
       responseObserver.onCompleted();
     }
   
     @Override
     public void greetingForEver(GreetingProtos.GreetingRequest request,
         StreamObserver<GreetingProtos.GreetingResponse> responseObserver) {
       super.greetingForEver(request, responseObserver);
     }
   }
   ```

3. Register gRPC Server to SkyLB Server

   ```java
   Server server = ServerTemplate.create(
   			{port} 9090, 
   			{bindableService} new DemoGrpcImpl(), 
   			{serviceName} "shared-test-client-service")
     .build()
     .start();
   
   SkyLBServiceReporter reporter = ServerTemplate.reportLoad(
   			{skylbUri} "skylb://127.0.0.1:1900/",
   			{serviceName} ServiceNameUtil.toString(ServiceId.CUSTOM_EASE_GATEWAY_TEST),
   			{portName} "grpc",
   			{port} 9090);
   ```

4. Call gRPC Server

   Create gRPC Stub

   ```java
   ManagedChannel channel = ClientTemplate.createChannel(
   			{skylbAddr} "skylb://127.0.0.1:1900/",
   			{calleeServiceName} ServiceNameUtil.toString(ServiceId.CUSTOM_EASE_GATEWAY_TEST),
   			{calleePortName} "grpc", 
   			{calleeNamespace} null,                                   
   			{callerServiceName} ServiceNameUtil.toString(ServiceId.SERVICE_NONE)).getOriginChannel();
   
   DemoGrpc.DemoBlockingStub blockingStub = DemoGrpc.newBlockingStub(channel);
   ```

   ```java
   GreetingRequest request = GreetingRequest.newBuilder()
     .setName("GC " + Calendar.getInstance().get(Calendar.SECOND))
     .build();
   GreetingResponse response = blockingStub
     .withDeadlineAfter(2000, TimeUnit.MILLISECONDS)
     .greeting(request);
   ```

## Run Demo

1. Build SkyLB Server

   ```shell
   chenbindeMacBook-Pro:grpc-skylb chenbin$ cd skylb-server
   chenbindeMacBook-Pro:skylb-server chenbin$ mvn clean package
   ```

   > 最终skylb.jar 打包到 skylb-server/target 目录下

2. Start SkyLB Server

   **Start ETCD3**

   ```shell
   yum install etcd -y
   
   systemctl enable etcd && systemctl start etcd
   ```

   > **NOTE**
   >
   > 部署ETCD遇到问题可参考 https://www.cnblogs.com/shawhe/p/10640820.html

   ```shell
   chenbindeMacBook-Pro:skylb-server chenbin$ cd target/skylblib
   
   chenbindeMacBook-Pro:skylblib chenbin$ java -jar skylb.jar -h
   	Usage: java -jar skylb.jar [options] [command] [command options]
     Options:
       --auto-disconn-timeout, -auto-disconn-timeout
         The timeout to automatically disconnect the resolve RPC. e.g. 10s(10 
         Seconds), 10m(10 Minutes)
         Default: PT5M
     Commands:
       etcd      Help for etcd options
         Usage: etcd [options]
           Options:
             --etcd-endpoints, -etcd-endpoints
               The comma separated ETCD endpoints. e.g., 
               http://etcd1:2379,http://etcd2:2379 
               Default: [http://127.0.0.1:2379]
             --etcd-key-ttl, -etcd-key-ttl
               The etcd key time-to-live. e.g. 10s(10 Seconds), 10m(10 Minutes)
               Default: PT10S
   
   chenbindeMacBook-Pro:skylblib chenbin$ java -jar skylb.jar -etcd-endpoints=http://127.0.0.1:2379
   ```

3. Start gRPC Server

   https://github.com/binchencoder/grpc-skylb/blob/master/examples/demo/src/main/java/com/binchencoder/skylb/demo/grpc/server/GreetingServer.java

   >  Run com/binchencoder/skylb/demo/grpc/server/GreetingServer#main

4. Start gRPC Client

   https://github.com/binchencoder/grpc-skylb/blob/master/examples/demo/src/main/java/com/binchencoder/skylb/demo/grpc/client/GreetingClient.java

   > Run com/binchencoder/skylb/demo/grpc/client/GreetingClient#main

## More Examples

grpc-skylb/examples/echo

该示例采用SpringBoot脚手架，在spring-boot-grpc-common.jar中封装好了注册服务的逻辑，启动方式比较简单

````java
@GrpcService(applyGlobalInterceptors = true) // Default use Global Interceptor
public class EchoGrpcService extends EchoServiceGrpc.EchoServiceImplBase {
}
````

1. 使用`@GrpcService` 注解方式标示gRPC Service

2. 在 spring-boot-grpc-common.jar 中默认配置两个Global Interceptor(ExceptionInterceptor, AuthenticationInterceptor)

3. 使用者可通过注解不使用Global Interceptor，实现自己的Interceptor

   ```java
   @GrpcService(applyGlobalInterceptors = false, interceptors = ExceptionInterceptor.class)
   ```

## Features

- @GrpcService Annotation
- 支持配置全局拦截器
- 将来计划支持skylb-client Golang版本

## References

- https://github.com/grpc/grpc/blob/master/doc/load-balancing.md
- [部署ETCD3集群](https://www.cnblogs.com/shawhe/p/10640820.html)