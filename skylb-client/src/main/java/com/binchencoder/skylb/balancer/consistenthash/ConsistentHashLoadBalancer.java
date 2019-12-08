package com.binchencoder.skylb.balancer.consistenthash;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.grpc.ConnectivityState.CONNECTING;
import static io.grpc.ConnectivityState.IDLE;
import static io.grpc.ConnectivityState.READY;
import static io.grpc.ConnectivityState.SHUTDOWN;
import static io.grpc.ConnectivityState.TRANSIENT_FAILURE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.binchencoder.skylb.healthcheck.Sizer;

import io.grpc.Attributes;
import io.grpc.ChannelLogger.ChannelLogLevel;
import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.Metadata;
import io.grpc.Metadata.Key;
import io.grpc.Status;
import io.grpc.internal.GrpcAttributes;
import io.grpc.internal.ServiceConfigUtil;

/**
 * Copied from io.grpc.util.RoundRobinLoadBalancer, for the original class
 * is a packaged and final class.
 */
final class ConsistentHashLoadBalancer extends LoadBalancer implements Sizer {
  private final Helper helper;
  private final Map<EquivalentAddressGroup, Subchannel> subchannels =
      new HashMap<EquivalentAddressGroup, Subchannel>();

  ConsistentHash hashChannels = new ConsistentHash(new Crc32Hash(), 20, Collections.EMPTY_LIST);

  @VisibleForTesting
  static final Attributes.Key<Ref<ConnectivityStateInfo>> STATE_INFO =
  Attributes.Key.create("state-info");

  private final Random random;
  // package-private to avoid synthetic access
  static final Attributes.Key<Ref<Subchannel>> STICKY_REF = Attributes.Key.create("sticky-ref");

  private static final Key<String> mdkey_hashkey = Key.of("mdkey_hashkey", Metadata.ASCII_STRING_MARSHALLER);

  private ConnectivityState currentState;
  private ConsistentHashPicker currentPicker = new EmptyPicker(EMPTY_OK);

  @Nullable
  private StickinessState stickinessState;

  ConsistentHashLoadBalancer(Helper helper) {
    this.helper = checkNotNull(helper, "helper");
    this.random = new Random();
  }

  @Override
  public void handleResolvedAddressGroups(
      List<EquivalentAddressGroup> servers, Attributes attributes) {
    Set<EquivalentAddressGroup> currentAddrs = subchannels.keySet();
    Set<EquivalentAddressGroup> latestAddrs = stripAttrs(servers);
    Set<EquivalentAddressGroup> addedAddrs = setsDifference(latestAddrs, currentAddrs);
    Set<EquivalentAddressGroup> removedAddrs = setsDifference(currentAddrs, latestAddrs);

    Map<String, Object> serviceConfig =
        attributes.get(GrpcAttributes.NAME_RESOLVER_SERVICE_CONFIG);
    if (serviceConfig != null) {
      String stickinessMetadataKey =
          ServiceConfigUtil.getStickinessMetadataKeyFromServiceConfig(serviceConfig);
      if (stickinessMetadataKey != null) {
        if (stickinessMetadataKey.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
          helper.getChannelLogger().log(
              ChannelLogLevel.WARNING,
              "Binary stickiness header is not supported. The header \"{0}\" will be ignored",
              stickinessMetadataKey);
        } else if (stickinessState == null
            || !stickinessState.key.name().equals(stickinessMetadataKey)) {
          stickinessState = new StickinessState(stickinessMetadataKey);
        }
      }
    }

    // Create new subchannels for new addresses.
    for (EquivalentAddressGroup addressGroup : addedAddrs) {
      // NB(lukaszx0): we don't merge `attributes` with `subchannelAttr` because subchannel
      // doesn't need them. They're describing the resolved server list but we're not taking
      // any action based on this information.
      Attributes.Builder subchannelAttrs = Attributes.newBuilder()
          // NB(lukaszx0): because attributes are immutable we can't set new value for the key
          // after creation but since we can mutate the values we leverage that and set
          // AtomicReference which will allow mutating state info for given channel.
          .set(STATE_INFO,
              new Ref<ConnectivityStateInfo>(ConnectivityStateInfo.forNonError(IDLE)));

      Ref<Subchannel> stickyRef = null;
      if (stickinessState != null) {
        subchannelAttrs.set(STICKY_REF, stickyRef = new Ref<Subchannel>(null));
      }

      Subchannel subchannel = checkNotNull(
          helper.createSubchannel(addressGroup, subchannelAttrs.build()), "subchannel");
      if (stickyRef != null) {
        stickyRef.value = subchannel;
      }

      subchannels.put(addressGroup, subchannel);
      hashChannels.add(subchannel); // Update hash ring.
      subchannel.requestConnection();
    }

    // Shutdown subchannels for removed addresses.
    for (EquivalentAddressGroup addressGroup : removedAddrs) {
      Subchannel subchannel = subchannels.remove(addressGroup);
      hashChannels.remove(subchannel); // Update hash ring.
      shutdownSubchannel(subchannel);
    }

    updateBalancingState();
  }

  @Override
  public void handleNameResolutionError(Status error) {
    // ready pickers aren't affected by status changes
    updateBalancingState(TRANSIENT_FAILURE,
        currentPicker instanceof ReadyPicker ? currentPicker : new EmptyPicker(error));
  }

  @Override
  public void handleSubchannelState(Subchannel subchannel, ConnectivityStateInfo stateInfo) {
    if (subchannels.get(subchannel.getAddresses()) != subchannel) {
      return;
    }
    if (stateInfo.getState() == SHUTDOWN && stickinessState != null) {
      stickinessState.remove(subchannel);
    }
    if (stateInfo.getState() == IDLE) {
      subchannel.requestConnection();
    }
    getSubchannelStateInfoRef(subchannel).value = stateInfo;
    updateBalancingState();
  }

  private void shutdownSubchannel(Subchannel subchannel) {
    subchannel.shutdown();
    getSubchannelStateInfoRef(subchannel).value =
        ConnectivityStateInfo.forNonError(SHUTDOWN);
    if (stickinessState != null) {
      stickinessState.remove(subchannel);
    }
  }

  @Override
  public void shutdown() {
    for (Subchannel subchannel : getSubchannels()) {
      shutdownSubchannel(subchannel);
    }
  }

  private static final Status EMPTY_OK = Status.OK.withDescription("no subchannels ready");

  /**
   * Updates picker with the list of active subchannels (state == READY).
   */
  private void updateBalancingState() {
    List<Subchannel> activeList = filterNonFailingSubchannels(getSubchannels());
    if (activeList.isEmpty()) {
      // No READY subchannels, determine aggregate state and error status
      boolean isConnecting = false;
      Status aggStatus = EMPTY_OK;
      for (Subchannel subchannel : getSubchannels()) {
        ConnectivityStateInfo stateInfo = getSubchannelStateInfoRef(subchannel).value;
        // This subchannel IDLE is not because of channel IDLE_TIMEOUT,
        // in which case LB is already shutdown.
        // RRLB will request connection immediately on subchannel IDLE.
        if (stateInfo.getState() == CONNECTING || stateInfo.getState() == IDLE) {
          isConnecting = true;
        }
        if (aggStatus == EMPTY_OK || !aggStatus.isOk()) {
          aggStatus = stateInfo.getStatus();
        }
      }
      updateBalancingState(isConnecting ? CONNECTING : TRANSIENT_FAILURE,
          // If all subchannels are TRANSIENT_FAILURE, return the Status associated with
          // an arbitrary subchannel, otherwise return OK.
          new EmptyPicker(aggStatus));
    } else {
      // initialize the Picker to a random start index to ensure that a high frequency of Picker
      // churn does not skew subchannel selection.
      int startIndex = random.nextInt(activeList.size());
      updateBalancingState(READY, new ReadyPicker(activeList, startIndex, stickinessState, this.hashChannels));
    }
  }

  private void updateBalancingState(ConnectivityState state, ConsistentHashPicker picker) {
    if (state != currentState || !picker.isEquivalentTo(currentPicker)) {
      helper.updateBalancingState(state, picker);
      currentState = state;
      currentPicker = picker;
    }
  }

  /**
   * Filters out non-ready subchannels.
   */
  private static List<Subchannel> filterNonFailingSubchannels(
      Collection<Subchannel> subchannels) {
    List<Subchannel> readySubchannels = new ArrayList<>(subchannels.size());
    for (Subchannel subchannel : subchannels) {
      if (isReady(subchannel)) {
        readySubchannels.add(subchannel);
      }
    }
    return readySubchannels;
  }

  /**
   * Converts list of {@link EquivalentAddressGroup} to {@link EquivalentAddressGroup} set and
   * remove all attributes.
   */
  private static Set<EquivalentAddressGroup> stripAttrs(List<EquivalentAddressGroup> groupList) {
    Set<EquivalentAddressGroup> addrs = new HashSet<EquivalentAddressGroup>(groupList.size());
    for (EquivalentAddressGroup group : groupList) {
      addrs.add(new EquivalentAddressGroup(group.getAddresses()));
    }
    return addrs;
  }

  @VisibleForTesting
  Collection<Subchannel> getSubchannels() {
    return subchannels.values();
  }

  private static Ref<ConnectivityStateInfo> getSubchannelStateInfoRef(
      Subchannel subchannel) {
    return checkNotNull(subchannel.getAttributes().get(STATE_INFO), "STATE_INFO");
  }

  // package-private to avoid synthetic access
  static boolean isReady(Subchannel subchannel) {
    return getSubchannelStateInfoRef(subchannel).value.getState() == READY;
  }

  private static <T> Set<T> setsDifference(Set<T> a, Set<T> b) {
    Set<T> aCopy = new HashSet<T>(a);
    aCopy.removeAll(b);
    return aCopy;
  }

  Map<String, Ref<Subchannel>> getStickinessMapForTest() {
    if (stickinessState == null) {
      return null;
    }
    return stickinessState.stickinessMap;
  }

  /**
   * Holds stickiness related states: The stickiness key, a registry mapping stickiness values to
   * the associated Subchannel Ref, and a map from Subchannel to Subchannel Ref.
   */
  @VisibleForTesting
  static final class StickinessState {
    static final int MAX_ENTRIES = 1000;

    final Key<String> key;
    final ConcurrentMap<String, Ref<Subchannel>> stickinessMap =
        new ConcurrentHashMap<String, Ref<Subchannel>>();

    final Queue<String> evictionQueue = new ConcurrentLinkedQueue<String>();

    StickinessState(@Nonnull String stickinessKey) {
      this.key = Key.of(stickinessKey, Metadata.ASCII_STRING_MARSHALLER);
    }

    /**
     * Returns the subchannel associated to the stickiness value if available in both the
     * registry and the round robin list, otherwise associates the given subchannel with the
     * stickiness key in the registry and returns the given subchannel.
     */
    @Nonnull
    Subchannel maybeRegister(
        String stickinessValue, @Nonnull Subchannel subchannel) {
      final Ref<Subchannel> newSubchannelRef = subchannel.getAttributes().get(STICKY_REF);
      while (true) {
        Ref<Subchannel> existingSubchannelRef =
            stickinessMap.putIfAbsent(stickinessValue, newSubchannelRef);
        if (existingSubchannelRef == null) {
          // new entry
          addToEvictionQueue(stickinessValue);
          return subchannel;
        } else {
          // existing entry
          Subchannel existingSubchannel = existingSubchannelRef.value;
          if (existingSubchannel != null && isReady(existingSubchannel)) {
            return existingSubchannel;
          }
        }
        // existingSubchannelRef is not null but no longer valid, replace it
        if (stickinessMap.replace(stickinessValue, existingSubchannelRef, newSubchannelRef)) {
          return subchannel;
        }
        // another thread concurrently removed or updated the entry, try again
      }
    }

    private void addToEvictionQueue(String value) {
      String oldValue;
      while (stickinessMap.size() >= MAX_ENTRIES && (oldValue = evictionQueue.poll()) != null) {
        stickinessMap.remove(oldValue);
      }
      evictionQueue.add(value);
    }

    /**
     * Unregister the subchannel from StickinessState.
     */
    void remove(Subchannel subchannel) {
      subchannel.getAttributes().get(STICKY_REF).value = null;
    }

    /**
     * Gets the subchannel associated with the stickiness value if there is.
     */
    @Nullable
    Subchannel getSubchannel(String stickinessValue) {
      Ref<Subchannel> subchannelRef = stickinessMap.get(stickinessValue);
      if (subchannelRef != null) {
        return subchannelRef.value;
      }
      return null;
    }
  }

  // Only subclasses are ReadyPicker or EmptyPicker
  private abstract static class ConsistentHashPicker extends SubchannelPicker {
    abstract boolean isEquivalentTo(ConsistentHashPicker picker);
  }

  @VisibleForTesting
  static final class ReadyPicker extends ConsistentHashPicker {
    private static final AtomicIntegerFieldUpdater<ReadyPicker> indexUpdater =
        AtomicIntegerFieldUpdater.newUpdater(ReadyPicker.class, "index");

    private final List<Subchannel> list; // non-empty
    @Nullable
    private final ConsistentHashLoadBalancer.StickinessState stickinessState;
    private final ConsistentHash hashChannels;
    @SuppressWarnings("unused")
    private volatile int index;

    ReadyPicker(List<Subchannel> list, int startIndex,
        @Nullable ConsistentHashLoadBalancer.StickinessState stickinessState,
        ConsistentHash hashChannels) {
      Preconditions.checkArgument(!list.isEmpty(), "empty list");
      this.list = list;
      this.stickinessState = stickinessState;
      this.index = startIndex - 1;
      this.hashChannels = hashChannels;
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
      Subchannel subchannel = null;
      final String hashKey = args.getHeaders().get(mdkey_hashkey);
      if (stickinessState != null) {
        String stickinessValue = args.getHeaders().get(stickinessState.key);
        if (stickinessValue != null) {
          subchannel = stickinessState.getSubchannel(stickinessValue);
          if (subchannel == null || !ConsistentHashLoadBalancer.isReady(subchannel)) {
            subchannel = stickinessState.maybeRegister(stickinessValue, nextSubchannel(hashKey));
          }
        }
      }

      return PickResult.withSubchannel(subchannel != null ? subchannel : nextSubchannel(hashKey));
    }

    private Subchannel nextSubchannel(String hashKey) {
      if (hashKey != null && !"".equals(hashKey)) {
        return hashChannels.get(hashKey);
      }
      int size = list.size();
      int i = indexUpdater.incrementAndGet(this);
      if (i >= size) {
        int oldi = i;
        i %= size;
        indexUpdater.compareAndSet(this, oldi, i);
      }
      return list.get(i);
    }

    @VisibleForTesting
    List<Subchannel> getList() {
      return list;
    }

    @Override
    boolean isEquivalentTo(ConsistentHashPicker picker) {
      if (!(picker instanceof ReadyPicker)) {
        return false;
      }
      ReadyPicker other = (ReadyPicker) picker;
      // the lists cannot contain duplicate subchannels
      return other == this || (stickinessState == other.stickinessState
          && list.size() == other.list.size()
          && new HashSet<Subchannel>(list).containsAll(other.list));
    }
  }

  @VisibleForTesting
  static final class EmptyPicker extends ConsistentHashPicker {

    private final Status status;

    EmptyPicker(@Nonnull Status status) {
      this.status = Preconditions.checkNotNull(status, "status");
    }

    @Override
    public PickResult pickSubchannel(PickSubchannelArgs args) {
      return status.isOk() ? PickResult.withNoResult() : PickResult.withError(status);
    }

    @Override
    boolean isEquivalentTo(ConsistentHashPicker picker) {
      return picker instanceof EmptyPicker && (Objects.equal(status, ((EmptyPicker) picker).status)
          || (status.isOk() && ((EmptyPicker) picker).status.isOk()));
    }
  }

  /**
   * A lighter weight Reference than AtomicReference.
   */
  @VisibleForTesting
  static final class Ref<T> {
    T value;

    Ref(T value) {
      this.value = value;
    }
  }

  @Override
  public int getSize() {
    return this.subchannels.size();
  }
}
