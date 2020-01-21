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

/**
 * Request to fetch a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchPartitionObjectsRequest extends NamedUserIdObjectOrHashRequest
{
  private final boolean         scanForwards_;
  private final String          after_;
  private final String          sortKeyPrefix_;
  private final AbstractConsumerManager consumerManager_;
  
  /**
   * Constructor.
   */
  FetchPartitionObjectsRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);

    scanForwards_     = builder.scanForwards_;
    after_            = builder.after_;
    sortKeyPrefix_    = builder.sortKeyPrefix_;
    consumerManager_  = builder.consumerManager_;
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
   * @return The ConsumerManager to receive objects.
   */
  public AbstractConsumerManager getConsumerManager()
  {
    return consumerManager_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, FetchPartitionObjectsRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchPartitionObjectsRequest construct()
    {
      return new FetchPartitionObjectsRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchPartitionObjectsRequest> extends NamedUserIdObjectOrHashRequest.AbstractBuilder<T,B>
  {
    protected boolean         scanForwards_ = true;
    protected String          after_;
    protected String          sortKeyPrefix_;
    protected AbstractConsumerManager consumerManager_;
    
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
     * Set the after of the partition.
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
     * Set the ConsumerManager to receive objects.
     * 
     * @param consumerManager The ConsumerManager to receive objects.
     * 
     * @return This (fluent method)
     */
    public T withConsumerManager(AbstractConsumerManager consumerManager)
    {
      consumerManager_ = consumerManager;
      
      return self();
    }
  }
}
