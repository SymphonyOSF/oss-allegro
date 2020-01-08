/*
 * Copyright 2019-2020 Symphony Communication Services, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.symphony.oss.allegro.api.request;

import java.util.HashSet;
import java.util.Set;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.hash.Hash;

import com.google.common.collect.ImmutableSet;
import com.symphony.oss.allegro.api.ResourcePermissions;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;

/**
 * Request object for UpsertFeed.
 * 
 * @author Bruce Skingle
 *
 */
public class UpsertFeedRequest extends NamedUserIdObjectRequest
{
  private final ImmutableSet<NamedUserIdObjectOrHashRequest> partitionIds_;
  
  UpsertFeedRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    partitionIds_  = ImmutableSet.copyOf(builder.partitionIds_);
  }
  
  /**
   * 
   * @param defaultOwner The default owner for partitions.
   * 
   * @return The partitions to which the feed should be subscribed.
   */
  public Set<Hash> getPartitionHashes(PodAndUserId defaultOwner)
  {
    Set<Hash> hashes = new HashSet<>();
    
    for(NamedUserIdObjectOrHashRequest id : partitionIds_)
      hashes.add(id.getHash(defaultOwner));
    
    return hashes;
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, UpsertFeedRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected UpsertFeedRequest construct()
    {
      return new UpsertFeedRequest(this);
    }
  }

  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends UpsertFeedRequest> extends NamedUserIdObjectRequest.AbstractBuilder<T,B>
  {
    protected Set<NamedUserIdObjectOrHashRequest> partitionIds_ = new HashSet<>();
    protected ResourcePermissions                         permissions_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    /**
     * Add the given partition IDs to the set of partitions to be subscribed to.
     * 
     * @param partitionId partition IDs to the set of partitions to be subscribed to.
     * 
     * @return This (fluent method)
     */
    public T withPartitionIds(NamedUserIdObjectOrHashRequest ...partitionId)
    {
      for(NamedUserIdObjectOrHashRequest id : partitionId)
        partitionIds_.add(id);
      
      return self();
    }

    /**
     * Add the given partition hashes to the set of partitions o be subscribed to.
     * 
     * @param partitionHashes partition hashes to the set of partitions o be subscribed to.
     * 
     * @return This (fluent method)
     */
    public T withPartitionHashes(Hash ...partitionHashes)
    {
      for(Hash partitionHash : partitionHashes)
        partitionIds_.add(new PartitionId.Builder().withHash(partitionHash).build());
      
      return self();
    }

    public void withPermissions(ResourcePermissions permissions)
    {
      permissions_ = permissions;
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(name_, "Name");
    }
  }
}
