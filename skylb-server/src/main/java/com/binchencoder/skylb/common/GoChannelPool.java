package com.binchencoder.skylb.common;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

public class GoChannelPool {

  private final static GoChannelPool defaultInstance = newPool();

  private final AtomicLong serialNumber = new AtomicLong();
  private final WeakHashMap<Long, WeakReference<GoChannel>> channelWeakHashMap = new WeakHashMap<>();
  private final LinkedBlockingDeque<GoChannelObject> totalQueue = new LinkedBlockingDeque<>();

  public <T> GoChannel<T> newChannel() {
    GoChannel<T> channel = new GoChannel<>();
    channelWeakHashMap.put(channel.getId(), new WeakReference<>(channel));
    return channel;
  }

  public <T> GoChannel<T> newChannel(int capacityDeq) {
    GoChannel<T> channel = new GoChannel<>(capacityDeq);
    channelWeakHashMap.put(channel.getId(), new WeakReference<>(channel));
    return channel;
  }

  public void select(GoSelectConsumer consumer) throws InterruptedException {
    consumer.accept(getTotalQueue().take());
  }

  public int size() {
    return getTotalQueue().size();
  }

  public int getChannelCount() {
    return channelWeakHashMap.values().size();
  }

  private LinkedBlockingDeque<GoChannelObject> getTotalQueue() {
    return totalQueue;
  }

  public static GoChannelPool getDefaultInstance() {
    return defaultInstance;
  }

  public static GoChannelPool newPool() {
    return new GoChannelPool();
  }

  private GoChannelPool() {
  }

  private long getSerialNumber() {
    return serialNumber.getAndIncrement();
  }

  private synchronized void syncTakeAndDispatchObject() throws InterruptedException {
    select(new GoSelectConsumer() {
      @Override
      void accept(GoChannelObject t) {
        WeakReference<GoChannel> goChannelWeakReference = channelWeakHashMap.get(t.channel_id);
        GoChannel channel = goChannelWeakReference != null ? goChannelWeakReference.get() : null;
        if (channel != null) {
          channel.offerBuffer(t);
        }
      }
    });
  }

  public class GoChannel<E> {

    // Instance
    private final long id;
    private final LinkedBlockingDeque<GoChannelObject<E>> buffer;

    public GoChannel() {
      this(getSerialNumber());
    }

    private GoChannel(long id) {
      this.id = id;
      this.buffer = new LinkedBlockingDeque<>();
    }

    private GoChannel(int capacityDeq) {
      this.id = getSerialNumber();
      this.buffer = new LinkedBlockingDeque<>(capacityDeq);
    }

    public long getId() {
      return id;
    }

    public E take() throws InterruptedException {
      GoChannelObject object;
      while ((object = pollBuffer()) == null) {
        syncTakeAndDispatchObject();
      }

      return (E) object.data;
    }

    public void offer(E object) {
      GoChannelObject<E> e = new GoChannelObject();
      e.channel_id = getId();
      e.data = object;

      getTotalQueue().offer(e);
    }

    protected void offerBuffer(GoChannelObject<E> data) {
      buffer.offer(data);
    }

    protected GoChannelObject<E> pollBuffer() {
      return buffer.poll();
    }

    public int size() {
      return buffer.size();
    }

    public void close() throws Throwable {
      this.finalize();
    }

    public boolean isStoped() {
      return !channelWeakHashMap.containsKey(getId());
    }

    @Override
    protected void finalize() throws Throwable {
      super.finalize();

      channelWeakHashMap.remove(getId());
    }
  }

  class GoChannelObject<E> {

    long channel_id;
    E data;

    boolean belongsTo(GoChannel channel) {
      return channel != null && channel_id == channel.id;
    }
  }

  abstract static class GoSelectConsumer {

    abstract void accept(GoChannelObject t);
  }

}
