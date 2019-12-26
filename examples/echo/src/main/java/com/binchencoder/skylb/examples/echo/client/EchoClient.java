package com.binchencoder.skylb.examples.echo.client;

import com.binchencoder.easegw.examples.EchoServiceGrpc;
import com.binchencoder.easegw.examples.ExamplesProto.SimpleMessage;
import com.binchencoder.grpc.data.DataProtos.ServiceId;
import com.binchencoder.grpc.utils.ServiceNameUtil;
import com.binchencoder.skylb.grpc.ClientTemplate;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoClient {

  private static Logger logger = LoggerFactory.getLogger(EchoClient.class);

  private static ManagedChannel channel;
  private static EchoServiceGrpc.EchoServiceBlockingStub blockingStub;

  /**
   * Construct client connecting to Echo server at {@code host:port}.
   */
  // 首先, 我们需要为stub创建一个grpc的channel, 指定我们连接服务端的地址和端口
  // 使用ManagedChannelBuilder方法来创建channel
  public EchoClient() {
    channel = ClientTemplate.createChannel("skylb://localhost:1900/",
        ServiceNameUtil.toString(ServiceId.CUSTOM_EASE_GATEWAY_TEST),
        "grpc", null,
        ServiceNameUtil.toString(ServiceId.SERVICE_NONE)).getOriginChannel();

    blockingStub = EchoServiceGrpc.newBlockingStub(channel)
        .withDeadlineAfter(20000, TimeUnit.MILLISECONDS);
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting.
   */
  public static void main(String[] args) throws InterruptedException {
    EchoClient client = new EchoClient();

    CountDownLatch latch = new CountDownLatch(1);
    try {
      String user = "world";
      for (int i = 0; i < 100; i++) {
//        client.echo(user);
        new Thread(() -> {
          client.echo(user);
        }).start();
      }

      latch.await();
      latch.countDown();
    } finally {
      client.shutdown();
    }
  }

  private void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  private void echo(String name) {
    logger.info("Will try to echo " + name + "...");

    long start = System.currentTimeMillis();
    SimpleMessage request = SimpleMessage.newBuilder().setId(name).build();
    SimpleMessage resp;

    try {
      resp = blockingStub.echoBody(request);
    } catch (StatusRuntimeException ex) {
      logger.error("RPC failed: {}", ex.getStatus());
      return;
    }

    logger
        .info("Greeting, response message is: {}, cost time: {}. ", resp.toBuilder().buildPartial(),
            (System.currentTimeMillis() - start));
  }
}
