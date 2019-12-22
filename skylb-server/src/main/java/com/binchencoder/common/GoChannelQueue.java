package com.binchencoder.common;

import com.google.common.collect.Sets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class GoChannelQueue<E> {

  private Timer timer;

  private final ConcurrentLinkedQueue<E> buffer;
  private AtomicBoolean closed; // 不能写入也不能消费了
  private AtomicBoolean stoped; // 停止写入队列, 还能继续消费

  /** Lock held by take */
  private final ReentrantLock takeLock = new ReentrantLock();
  /** Wait queue for waiting takes */
  private final Condition notEmpty = takeLock.newCondition();

  public GoChannelQueue() {
    this.buffer = new ConcurrentLinkedQueue();
    this.stoped = new AtomicBoolean();
    this.closed = new AtomicBoolean();
  }

  public GoChannelQueue(E... buffer) {
    this.buffer = new ConcurrentLinkedQueue(Sets.newHashSet(buffer));
  }

  public boolean offer(E e) throws InterruptedException {
    if (this.closed.get()) {
      throw new InterruptedException("Send on closed queue.");
    }
    if (this.stoped.get()) {
      throw new InterruptedException("The queue is stoped, cannot be offer.");
    }

    return this.buffer.offer(e);
  }

  public E take() throws InterruptedException {
    if (this.closed.get()) {
      return null;
    }

    final ReentrantLock takeLock = this.takeLock;
    takeLock.lockInterruptibly();
    E data;
    try {
      while ((data = this.buffer.poll()) == null && !this.closed.get()) {
        notEmpty.await();
      }
    } finally {
      takeLock.unlock();
    }

    return data;
  }

  public int size() {
    return this.buffer.size();
  }

  public synchronized boolean close(long closeDelayInMs) {
    if (this.stoped.compareAndSet(false, true)) {
      if (closeDelayInMs <= 0) {
        this.closed.set(true);
        this.signalNotEmpty();
      } else {
        timer = new Timer();
        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            closed.set(true);
            signalNotEmpty();
            this.cancel();
          }
        }, closeDelayInMs);
      }

      return true;
    }

    this.buffer.clear();
    return false;
  }

  /**
   * Signals a waiting take.
   */
  private void signalNotEmpty() {
    final ReentrantLock takeLock = this.takeLock;
    takeLock.lock();
    try {
      notEmpty.signal();
    } finally {
      takeLock.unlock();
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    if (null != this.timer) {
      this.timer.cancel();
    }
  }
}