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

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;

/**
 * Request to fetch a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchFeedObjectsRequest
{
  private final String          name_;
  private final Integer         maxItems_;
  private final ConsumerManager consumerManager_;
  
  /**
   * Constructor.
   */
  FetchFeedObjectsRequest(AbstractBuilder<?,?> builder)
  {
    name_            = builder.name_;
    maxItems_         = builder.maxItems_;
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
   * @return The name of the feed to read from.
   */
  public String getName()
  {
    return name_;
  }

  /**
   * 
   * @return The ConsumerManager to receive objects.
   */
  public ConsumerManager getConsumerManager()
  {
    return consumerManager_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, FetchFeedObjectsRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchFeedObjectsRequest construct()
    {
      return new FetchFeedObjectsRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchFeedObjectsRequest> extends BaseAbstractBuilder<T,B>
  {
    protected String          name_;
    protected Integer         maxItems_;
    protected ConsumerManager consumerManager_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the name of the partition.
     * 
     * @param name The content type for the sequence.
     * 
     * @return This (fluent method)
     */
    public T withName(String name)
    {
      name_ = name;
      
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
    public T withConsumerManager(ConsumerManager consumerManager)
    {
      consumerManager_ = consumerManager;
      
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
