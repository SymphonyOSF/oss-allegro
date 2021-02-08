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

package com.symphony.oss.allegro2.api;

import com.symphony.oss.commons.fault.FaultAccumulator;

/**
 * Request to fetch a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchThreadObjectsRequest extends ThreadRequest
{
  private final Integer         maxItems_;
  private final String          after_;
  private final AllegroConsumerManager consumerManager_;
  
  /**
   * Constructor.
   */
  FetchThreadObjectsRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);

    maxItems_         = builder.maxItems_;
    after_            = builder.after_;
    consumerManager_  = builder.consumerManager_;
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
   * 
   * @return The paging marker to start from.
   */
  public String getAfter()
  {
    return after_;
  }

  /**
   * 
   * @return The ConsumerManager to receive objects.
   */
  public AllegroConsumerManager getConsumerManager()
  {
    return consumerManager_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, FetchThreadObjectsRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchThreadObjectsRequest construct()
    {
      return new FetchThreadObjectsRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends ThreadRequest> extends ThreadRequest.AbstractBuilder<T,B>
  {
    protected Integer         maxItems_;
    protected String          after_;
    protected AllegroConsumerManager consumerManager_;
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
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
    
    /**
     * Set the ConsumerManager to receive objects.
     * 
     * @param consumerManager The ConsumerManager to receive objects.
     * 
     * @return This (fluent method)
     */
    public T withConsumerManager(AllegroConsumerManager consumerManager)
    {
      consumerManager_ = consumerManager;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkNotNull(consumerManager_, "Consumer Manager");
      
      if(maxItems_ != null && maxItems_ < 1)
        faultAccumulator.error("maxItems must be at least 1, or not set.");
    }
  }
}
