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

import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.commons.hash.Hash;

/**
 * Request to fetch versions of a logical object by its baseHash.
 * 
 * @author Bruce Skingle
 *
 */
public class VersionQuery
{
  private final Hash            basehash_;
  private final boolean         scanForwards_;
  private final Integer         maxItems_;
  private final String          after_;
  
  /**
   * Constructor.
   */
  VersionQuery(AbstractBuilder<?,?> builder)
  {
    basehash_         = builder.basehash_;
    scanForwards_     = builder.scanForwards_;
    maxItems_         = builder.maxItems_;
    after_            = builder.after_;
  }
  
  /**
   * Return the basehash of the required object.
   * 
   * @return The basehash of the required object.
   */
  public Hash getBaseHash()
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
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, VersionQuery>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected VersionQuery construct()
    {
      return new VersionQuery(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends VersionQuery> extends BaseAbstractBuilder<T,B>
  {
    protected Hash            basehash_;
    protected boolean         scanForwards_ = true;
    protected Integer         maxItems_;
    protected String          after_;
    
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
