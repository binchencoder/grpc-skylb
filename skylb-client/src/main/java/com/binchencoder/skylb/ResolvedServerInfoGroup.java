package com.binchencoder.skylb;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Objects;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.ExperimentalApi;
import io.grpc.NameResolver;

/**
 * A group of {@link ResolvedServerInfo}s that is returned from a {@link NameResolver}.
 *
 * Copied from io.grpc.
 */
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/1770")
@Immutable
public final class ResolvedServerInfoGroup {
  private final List<ResolvedServerInfo> resolvedServerInfoList;
  private final Attributes attributes;

  /**
   * Constructs a new resolved server info group from {@link ResolvedServerInfo} list,
   * with custom {@link Attributes} attached to it.
   *
   * @param resolvedServerInfoList list of resolved server info objects.
   * @param attributes custom attributes for a given group.
   */
  private ResolvedServerInfoGroup(List<ResolvedServerInfo> resolvedServerInfoList,
      Attributes attributes) {
    checkArgument(!resolvedServerInfoList.isEmpty(), "empty server list");
    this.resolvedServerInfoList =
        Collections.unmodifiableList(new ArrayList<ResolvedServerInfo>(resolvedServerInfoList));
    this.attributes = checkNotNull(attributes, "attributes");
  }

  /**
   * Returns immutable list of {@link ResolvedServerInfo} objects for this group.
   */
  public List<ResolvedServerInfo> getResolvedServerInfoList() {
    return resolvedServerInfoList;
  }

  /**
   * Returns {@link Attributes} for this group.
   */
  public Attributes getAttributes() {
    return attributes;
  }

  /**
   * Converts this group to {@link EquivalentAddressGroup} object.
   */
  public EquivalentAddressGroup toEquivalentAddressGroup() {
    List<SocketAddress> addrs = new ArrayList<SocketAddress>(resolvedServerInfoList.size());
    for (ResolvedServerInfo resolvedServerInfo : resolvedServerInfoList) {
      addrs.add(resolvedServerInfo.getAddress());
    }
    return new EquivalentAddressGroup(addrs);
  }

  /**
   * Creates a new builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new builder for a group with extra attributes.
   */
  public static Builder builder(Attributes attributes) {
    return new Builder(attributes);
  }

  /**
   * Returns true if the given object is also a {@link ResolvedServerInfoGroup} with an equal
   * attributes and list of {@link ResolvedServerInfo} objects.
   *
   * @param o an object.
   * @return true if the given object is a {@link ResolvedServerInfoGroup} with an equal attributes
   *     and server info list.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResolvedServerInfoGroup that = (ResolvedServerInfoGroup) o;
    return Objects.equal(resolvedServerInfoList, that.resolvedServerInfoList)
        && Objects.equal(attributes, that.attributes);
  }

  /**
   * Returns a hash code for the resolved server info group.
   *
   * <p>Note that if a resolver includes mutable values in the attributes, this object's hash code
   * could change over time. So care must be used when putting these objects into a set or using
   * them as keys for a map.
   *
   * @return a hash code for the server info group.
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(resolvedServerInfoList, attributes);
  }

  @Override
  public String toString() {
    return "[servers=" + resolvedServerInfoList + ", attrs=" + attributes + "]";
  }

  /**
   * Builder for a {@link ResolvedServerInfo}.
   */
  public static final class Builder {
    private final List<ResolvedServerInfo> group = new ArrayList<ResolvedServerInfo>();
    private final Attributes attributes;

    public Builder(Attributes attributes) {
      this.attributes = attributes;
    }

    public Builder() {
      this(Attributes.EMPTY);
    }

    public Builder add(ResolvedServerInfo resolvedServerInfo) {
      group.add(resolvedServerInfo);
      return this;
    }

    public Builder addAll(Collection<ResolvedServerInfo> resolvedServerInfo) {
      group.addAll(resolvedServerInfo);
      return this;
    }

    public ResolvedServerInfoGroup build() {
      return new ResolvedServerInfoGroup(group, attributes);
    }
  }
}
