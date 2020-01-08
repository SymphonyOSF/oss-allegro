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

import java.util.HashSet;
import java.util.Set;

import org.symphonyoss.s2.common.fault.FaultAccumulator;

import com.google.common.collect.ImmutableSet;
import com.symphony.oss.models.core.canon.facade.ThreadId;

/**
 * Request to create a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class UpsertPartitionRequest extends NamedUserIdObjectRequest
{
  private final ImmutableSet<ThreadId> threadIds_;
  
  UpsertPartitionRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    threadIds_  = ImmutableSet.copyOf(builder.threadIds_);
  }
  
  /**
   * 
   * @return The allowable ThreadIds for this partition.
   */
  public Set<ThreadId> getThreadIds()
  {
    return threadIds_;
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, UpsertPartitionRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected UpsertPartitionRequest construct()
    {
      return new UpsertPartitionRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends UpsertPartitionRequest> extends NamedUserIdObjectRequest.AbstractBuilder<T,B>
  {
    protected Set<ThreadId> threadIds_ = new HashSet<>();
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    /**
     * Add the given thread IDs to the set of valid threadIds for this partition.
     * 
     * @param threadIds IDs of threads which can be used to encrypt objects in this partition.
     * 
     * @return This (fluent method)
     */
    public T withThreadIds(ThreadId ...threadIds)
    {
      for(ThreadId threadid : threadIds)
        threadIds_.add(threadid);
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
    }
  }
}
