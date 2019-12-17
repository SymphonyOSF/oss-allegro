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
public class FeedRequest
{
  private final String          name_;
  
  /**
   * Constructor.
   */
  FeedRequest(AbstractBuilder<?,?> builder)
  {
    name_            = builder.name_;
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
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FeedRequest> extends BaseAbstractBuilder<T,B>
  {
    protected String          name_;
    
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
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(name_, "Feed name");
    }
  }
}
