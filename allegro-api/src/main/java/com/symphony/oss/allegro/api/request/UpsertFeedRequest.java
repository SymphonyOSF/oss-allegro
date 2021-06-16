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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableSet;
import com.symphony.oss.allegro.api.ResourcePermissions;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.object.canon.IPartitionSelection;
import com.symphony.oss.models.object.canon.PartitionSelection;

/**
 * Request object for UpsertFeed.
 * 
 * @author Bruce Skingle
 *
 */
public class UpsertFeedRequest extends NamedUserIdObjectRequest
{
  private final ImmutableSet<PartitionSelectionRequest> partitionSelections_;
  private final boolean                                 subscribeMessages_;
  private final ResourcePermissions                     permissions_;
  protected final long                                  expiryTime_;
 
  private static final int  DYNAMODB_CLEANUP_DAYS = 2;
  private static final long TTL_LOWER_BOUND       = 1000 * 60;
  private static final long TTL_UPPER_BOUND       = 1000 * 60 * 60 * 24 * (7 - DYNAMODB_CLEANUP_DAYS);
  
  UpsertFeedRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    partitionSelections_  = ImmutableSet.copyOf(builder.partitionSelections_);
    subscribeMessages_    = builder.subscribeMessages_;
    permissions_          = builder.permissions_;
    expiryTime_           = builder.expiryTime_;   
  }
  
  /**
   * 
   * @param defaultOwner The default owner for partitions.
   * 
   * @return The partition selections to which the feed should be subscribed.
   */
  public List<IPartitionSelection> getPartitionSelections(PodAndUserId defaultOwner)
  {
    List<IPartitionSelection> partitionSelections = new ArrayList<>(partitionSelections_.size());
    
    for(PartitionSelectionRequest psr : partitionSelections_)
    {
      partitionSelections.add(new PartitionSelection.Builder()
          .withPartitionHash(psr.getPartitionId().getHash(defaultOwner))
          .withSortKeyPrefix(psr.getSortKeyPrefix())
          .build());
    }
    
    return partitionSelections;
  }
  
  /**
   * Return the subscribeMessages setting.
   * 
   * @return the subscribeMessages setting.
   */
  public boolean isSubscribeMessages()
  {
    return subscribeMessages_;
  }
  
  /**
   * 
   * @return The ResourcePermissions for the feed.
   */
  public ResourcePermissions getPermissions()
  {
    return permissions_;
  }
  
  /**
   * 
   * @return The ExpiryTime for the feed.
   */
  public long getExpiryTime()
  {
    return expiryTime_;
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
    protected Set<PartitionSelectionRequest>      partitionSelections_ = new HashSet<>();
    protected boolean                             subscribeMessages_;
    protected ResourcePermissions                 permissions_;
    protected long                                expiryTime_ = TTL_UPPER_BOUND;
    
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
    public T withPartitionIds(PartitionId ...partitionId)
    {
      for(PartitionId id : partitionId)
        partitionSelections_.add(new PartitionSelectionRequest.Builder()
            .withPartitionId(id)
            .build());
      
      return self();
    }
    
    /**
     * Set the subscribeMessages flag.
     * 
     * @param subscribeMessages true if messages to the creator should be subscribed for this feed.
     * 
     * @return This (fluent method)
     */
    public T withSubscribeMessages(boolean subscribeMessages)
    {
      subscribeMessages_ = subscribeMessages;
      
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
        partitionSelections_.add(new PartitionSelectionRequest.Builder()
            .withPartitionId(
                new PartitionId.Builder()
                .withHash(partitionHash)
                .build()
                )
            .build());
      
      return self();
    }

    /**
     * Add the given partition ID to the set of partitions to be subscribed to.
     * Only objects whose sortKey matches the given prefix will be forwarded.
     * 
     * @param partitionId   Partition IDs to be subscribed to.
     * @param sortKeyPrefix The prefix which the sort key of records must match if they are to be forwarded.
     * 
     * @return This (fluent method)
     */
    public T withPartitionSelection(PartitionId partitionId, String sortKeyPrefix)
    {
      partitionSelections_.add(new PartitionSelectionRequest.Builder()
          .withPartitionId(partitionId)
          .withSortKeyPrefix(sortKeyPrefix)
          .build());
    
      return self();
    }

    /**
     * Add the given partition hash to the set of partitions to be subscribed to.
     * Only objects whose sortKey matches the given prefix will be forwarded.
     * 
     * @param partitionHash partition hash to be subscribed to.
     * @param sortKeyPrefix The prefix which the sort key of records must match if they are to be forwarded.
     * 
     * @return This (fluent method)
     */
    public T withPartitionHashes(Hash partitionHash, String sortKeyPrefix)
    {
      partitionSelections_.add(new PartitionSelectionRequest.Builder()
          .withPartitionId(
              new PartitionId.Builder()
              .withHash(partitionHash)
              .build()
              )
          .withSortKeyPrefix(sortKeyPrefix)
          .build());
      
      return self();
    }

    /**
     * Set the given ResourcePermissions for the feed.
     * 
     * The Feed owner can always read the feed.
     * 
     * Applicable Permissions are:
     * 
     * None
     * Read
     * 
     * @param permissions ResourcePermissions.
     * 
     * @return This (fluent method) 
     */
    public T withPermissions(ResourcePermissions permissions)
    {
      permissions_ = permissions;
      
      return self();
    }
    
    /**
     * Set the TTL for the feed.
     * 
     * @param count integer.
     * @param unit TimeUnit.
     * 
     * @return This (fluent method) 
     */
    public T withExpiryTime(int count, TimeUnit unit)
    {
      expiryTime_ = TimeUnit.MILLISECONDS.convert(count, unit);

      if (expiryTime_ < TTL_LOWER_BOUND)
         throw new IllegalArgumentException("Expiry Time must be at least 60 seconds");
      else if (expiryTime_ > TTL_UPPER_BOUND)
         throw new IllegalArgumentException("Expiry Time must be less than "+(7 - DYNAMODB_CLEANUP_DAYS)+" days");  

      return self();
    }

    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(name_, "Name");
    }
  }
}
