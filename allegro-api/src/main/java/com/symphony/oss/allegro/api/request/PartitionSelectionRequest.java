/*
 *
 *
 * Copyright 2020 Symphony Communication Services, LLC.
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

import javax.annotation.Nullable;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;

/**
 * A Partition Selection which includes a Partition ID and an optional sort key prefix.
 * 
 * @author Bruce Skingle
 *
 */
public class PartitionSelectionRequest
{
  private final PartitionId partitionId_;
  private final String                         sortKeyPrefix_;

  PartitionSelectionRequest(AbstractBuilder<?,?> builder)
  {
    partitionId_ = builder.partitionId_;
    sortKeyPrefix_ = builder.sortKeyPrefix_;
  }

  /**
   * 
   * @return The partition ID.
   */
  public PartitionId getPartitionId()
  {
    return partitionId_;
  }

  /**
   * 
   * @return The optional sort key prefix or null.
   */
  public @Nullable String getSortKeyPrefix()
  {
    return sortKeyPrefix_;
  }

  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends PartitionSelectionRequest> extends BaseAbstractBuilder<T,B>
  {
    protected PartitionId partitionId_;
    protected String      sortKeyPrefix_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the partition ID.
     * 
     * @param partitionId The partition ID.
     * 
     * @return This (fluent method)
     */
    public T withPartitionId(PartitionId partitionId)
    {
      partitionId_ = partitionId;
      
      return self();
    }
    
    /**
     * Set the sortkey prefix.
     * 
     * @param sortKeyPrefix The sortKey Prefix.
     * 
     * @return This (fluent method)
     */
    public T withSortKeyPrefix(String sortKeyPrefix)
    {
      sortKeyPrefix_ = sortKeyPrefix;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkNotNull(partitionId_, "PartitionID");
    }
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, PartitionSelectionRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }
  
    @Override
    protected PartitionSelectionRequest construct()
    {
      return new PartitionSelectionRequest(this);
    }
  }
}
