/*
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

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.common.hash.Hash;

import com.symphony.oss.models.core.canon.facade.PodAndUserId;

/**
 * Request to fetch versions of a logical object by its baseHash.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchObjectVersionsRequest
{
  private final Hash            basehash_;
  private final boolean         scanForwards_;
  private final Integer         maxItems_;
  private final String          after_;
  private final ConsumerManager consumerManager_;
  
  /**
   * Constructor.
   */
  FetchObjectVersionsRequest(AbstractBuilder<?,?> builder)
  {
    basehash_ = builder.basehash_;
    scanForwards_     = builder.scanForwards_;
    maxItems_         = builder.maxItems_;
    after_            = builder.after_;
    consumerManager_  = builder.consumerManager_;
  }
  
  /**
   * Return the basehash of the required object.
   * 
   * @param defaultOwner The default value for the owner element of the ID.
   * 
   * @return The basehash of the required object.
   */
  public Hash getBaseHash(PodAndUserId defaultOwner)
  {
    return basehash_;
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
  public static class Builder extends AbstractBuilder<Builder, FetchObjectVersionsRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchObjectVersionsRequest construct()
    {
      return new FetchObjectVersionsRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchObjectVersionsRequest> extends BaseAbstractBuilder<T,B>
  {
    protected Hash            basehash_;
    protected boolean         scanForwards_ = true;
    protected Integer         maxItems_;
    protected String          after_;
    protected ConsumerManager consumerManager_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the basehash of the ID object.
     * 
     * @param basehash The basehash of the ID object. 
     * 
     * @return This (fluent method)
     */
    public T withBaseHash(Hash basehash)
    {
      basehash_ = basehash;
      
      return self();
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
      
      faultAccumulator.checkNotNull(basehash_, "BaseHash");
    }
  }
}
