/*
 * Copyright 2019 Symphony Communication Services, LLC.
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

import com.symphony.oss.commons.fault.FaultAccumulator;

/**
 * A query specification to fetch objects from a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class PartitionQuery extends NamedUserIdObjectOrHashRequest
{
  private final boolean         scanForwards_;
  private final String          after_;
  private final String          sortKeyPrefix_;
  private final Integer         maxItems_;
  
  /**
   * Constructor.
   */
  PartitionQuery(AbstractBuilder<?,?> builder)
  {
    super(builder);

    scanForwards_     = builder.scanForwards_;
    after_            = builder.after_;
    sortKeyPrefix_    = builder.sortKeyPrefix_;
    maxItems_         = builder.maxItems_;
  }

  /**
   * 
   * @return The order of scan.
   */
  public Boolean getScanForwards()
  {
    return scanForwards_;
  }

  /**
   * 
   * @return The paging marker to start from.
   */
  public String getAfter()
  {
    return after_;
  }

  /**
   * 
   * @return The required prefix for the sort key value of returned objects.
   */
  public String getSortKeyPrefix()
  {
    return sortKeyPrefix_;
  }
  
  /**
   * 
   * @return The maximum number of objects to return.
   */
  public Integer getMaxItems()
  {
    return maxItems_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, PartitionQuery>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected PartitionQuery construct()
    {
      return new PartitionQuery(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends PartitionQuery> extends NamedUserIdObjectOrHashRequest.AbstractBuilder<T,B>
  {
    protected boolean         scanForwards_ = true;
    protected String          after_;
    protected String          sortKeyPrefix_;
    protected Integer         maxItems_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the direction of scan.
     * 
     * @param scanForwards If true then scan forwards, else scan in the reverse order of sort keys.
     * 
     * @return This (fluent method)
     */
    public T withScanForwards(boolean scanForwards)
    {
      scanForwards_ = scanForwards;
      
      return self();
    }
    
    /**
     * Set the continuation token for a paged query.
     * 
     * @param after The paging marker to start from.
     * 
     * @return This (fluent method)
     */
    public T withAfter(String after)
    {
      after_ = after;
      
      return self();
    }
    
    /**
     * Set the after of the partition.
     * 
     * @param sortKeyPrefix The required prefix for sort key value of returned objects.
     * 
     * @return This (fluent method)
     */
    public T withSortKeyPrefix(String sortKeyPrefix)
    {
      sortKeyPrefix_ = sortKeyPrefix;
      
      return self();
    }
    
    /**
     * Set the maximum number of objects to return.
     * 
     * @param maxItems The maximum number of objects to return.
     * 
     * @return This (fluent method)
     */
    public T withMaxItems(Integer maxItems)
    {
      maxItems_ = maxItems;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      if(maxItems_ != null && maxItems_ < 1)
        faultAccumulator.error("maxItems must be at least 1, or not set.");
    }
  }
}
