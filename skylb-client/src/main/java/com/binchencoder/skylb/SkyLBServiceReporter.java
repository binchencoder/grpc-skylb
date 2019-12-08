package com.binchencoder.skylb;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.SettableFuture;

import com.binchencoder.skylb.metrics.Configuration;
import com.binchencoder.skylb.metrics.MetricsClientInterceptor;
import com.binchencoder.skylb.proto.ClientProtos.ReportLoadRequest;
import com.binchencoder.skylb.proto.ClientProtos.ReportLoadResponse;
import com.binchencoder.skylb.proto.ClientProtos.ServiceSpec;
import com.binchencoder.skylb.proto.SkylbGrpc;
import com.binchencoder.skylb.proto.SkylbGrpc.SkylbStub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.stub.StreamObserver;

public class SkyLBServiceReporter {
  private static final Logger logger = LoggerFactory.getLogger(SkyLBServiceReporter.class);

  private AtomicInteger curIndex;
  private List<InetSocketAddress> address;

  private ManagedChannel channel;
  private SkylbStub stub;

  private int reportInterval = 3000; // ms
  private Timer timer;

  private AtomicBoolean reqObserverOk = new AtomicBoolean(false);

  /**
   * @param addr skylb://127.0.0.1:1900,localhost:1900
   */
  public SkyLBServiceReporter(String skylb) {
    // Remove direct part.
    skylb = skylb.split(";")[0];

    URI uri = null;
    try {
      uri = new URI(skylb);
    } catch (URISyntaxException e) {
      logger.error("URI syntax error {}", skylb);
    }

    String authority = Preconditions.checkNotNull(uri.getAuthority(), "authority");
    this.address = SkyLBUtils.parseAddress(authority, SkyLBConst.SKYLB_PORT);
    Collections.shuffle(this.address);
    this.curIndex = new AtomicInteger(new Random().nextInt(this.address.size()));
    this.timer = new Timer(true);
  }

  public void setReportInterval(int reportInterval) {
    this.reportInterval = reportInterval;
  }

  public void shutdown() {
    this.timer.cancel();

    if (this.channel != null && !this.channel.isShutdown()) {
      this.channel.shutdownNow();
    }

    reqObserverOk.set(false);
  }

  private StreamObserver<ReportLoadRequest> reqObserver = null;

  /**
   * Register starts the SkyLB load-report worker with the given
   * service name and port.
   */
  public void register(String serviceName, String portName,
                       int port, ReportCallback cb) throws Exception {
    register(SkyLBConst.defaultNameSpace, serviceName, portName, port, cb);
  }

  public void register(String namespace, String serviceName, String portName,
                       final int port, final ReportCallback cb) throws Exception {

    // TODO(fuyc): change default value to true when most apps are k8s-ready.
    if (Objects.equal("true", Properties.withinK8s)) {
      logger.info("Spec [{}, {}, {}, {}] start within k8s, load-reporting disabled",
          namespace, serviceName, portName, port);
      return;
    }

    Preconditions.checkNotNull(serviceName, "service name");
    final ServiceSpec spec = SkyLBUtils.buildServiceSpec(namespace, serviceName, portName);
    if (spec == null) {
      throw new InvalidParameterException("service name is null.");
    }

    final String sn = serviceName;
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (!reqObserverOk.get()) {
          dial(sn, port);
        }

        if (reqObserverOk.get()) {
          reqObserver.onNext(ReportLoadRequest.newBuilder().setSpec(spec).setPort(port)
              .setWeight(cb.getWeight()).build());
          logger.trace("Report to skylb, service {}@{}.", sn, port);
        }
      }
    }, 0, reportInterval);

  }

  private InetSocketAddress dial(final String serviceName, final int port) {
    final InetSocketAddress addr =
        this.address.get(this.curIndex.getAndIncrement() % this.address.size());

    this.channel = ManagedChannelBuilder.forAddress(addr.getHostName(), addr.getPort())
        .nameResolverFactory(DnsNameResolverProvider.asFactory())
        .intercept(MetricsClientInterceptor.create(Configuration.allMetrics(),
            SkyLBConst.SKYLB_SERVER, SkyLBConst.SKYLB_REPORTER))
        .usePlaintext(true).build();
    this.stub = SkylbGrpc.newStub(this.channel);

    final CountDownLatch latch = new CountDownLatch(1);
    final SettableFuture<Void> finishFuture = SettableFuture.create();
    StreamObserver<ReportLoadResponse> respObserver = new StreamObserver<ReportLoadResponse>() {

      @Override
      public void onNext(ReportLoadResponse resp) {
        // nothing need to do.
        logger.debug("Reporter {}@{} received a response {}.", serviceName, port, resp.toString());
      }

      @Override
      public void onError(Throwable t) {
        finishFuture.setException(t);
        reqObserverOk.set(false);
        latch.countDown();
        logger.warn("Reporter {}@{} received an error {} from {}.",
            serviceName, port, t.getMessage(), addr);
      }

      @Override
      public void onCompleted() {
        finishFuture.set(null);
        timer.cancel();
        logger.debug("Reporter {}@{} completad.", serviceName, port);
      }

    };

    reqObserverOk.set(true);
    reqObserver = this.stub.reportLoad(respObserver);
    try {
      if (latch.await(reportInterval / 10, TimeUnit.MILLISECONDS)) {
        reqObserverOk.set(false);
      } else {
        logger.info("Reporter {}@{} dialled to {}.", serviceName, port, addr);
      }
    } catch (InterruptedException e) {
      logger.warn("Latch be interrupted.");
    }

    return addr;
  }
}
