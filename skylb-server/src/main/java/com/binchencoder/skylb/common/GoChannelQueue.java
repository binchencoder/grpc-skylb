package com.binchencoder.skylb.common;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class GoChannelQueue<E> {

  private Timer timer;

  private final LinkedBlockingQueue<E> buffer;
  private boolean closed; // 不能写入也不能消费了
  private boolean stoped; // 停止写入队列, 还能继续消费

  public GoChannelQueue() {
    this.buffer = new LinkedBlockingQueue<>();
  }

  public GoChannelQueue(int capacity) {
    this.buffer = new LinkedBlockingQueue<>(capacity);
  }

  public boolean offer(E e) throws InterruptedException {
    if (this.closed) {
      throw new InterruptedException("Send on closed queue.");
    }
    if (this.stoped) {
      throw new InterruptedException("The queue is stoped, cannot be offer.");
    }

    return this.buffer.offer(e);
  }

  public E take() throws InterruptedException {
    if (this.closed) {
      return null;
    }

    return this.buffer.take();
  }

  public int size() {
    return this.buffer.size();
  }

  public synchronized void close(long closeDelayInMs) {
    this.stoped = true;
    if (closeDelayInMs <= 0) {
      this.closed = true;
    } else {
      timer = new Timer();
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          closed = true;
          this.cancel();
          return;
        }
      }, closeDelayInMs);
    }

    this.buffer.clear();
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    if (null != this.timer) {
      this.timer.cancel();
    }
  }
}
