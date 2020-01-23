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

import java.util.LinkedList;
import java.util.List;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;

import com.google.common.collect.ImmutableList;

/**
 * Request to fetch versions of a logical object by its baseHash.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchObjectVersionsRequest
{
  private final ImmutableList<VersionQuery> queryList_;
  private final AbstractConsumerManager consumerManager_;
  
  /**
   * Constructor.
   */
  FetchObjectVersionsRequest(AbstractBuilder<?,?> builder)
  {

    queryList_        = ImmutableList.copyOf(builder.queryList_);
    consumerManager_  = builder.consumerManager_;
  }

  /**
   * 
   * @return The list of query specifications.
   */
  public ImmutableList<VersionQuery> getQueryList()
  {
    return queryList_;
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
    protected List<VersionQuery>      queryList_ = new LinkedList<>();
    protected AbstractConsumerManager consumerManager_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the direction of scan.
     * 
     * @param versionQuery A query specification to fetch object versions.
     * 
     * @return This (fluent method)
     */
    public T withQuery(VersionQuery versionQuery)
    {
      queryList_.add(versionQuery);
      
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
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      if(queryList_.isEmpty())
        faultAccumulator.error("At least 1 query must be provided.");
    }
  }
}
