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
import org.symphonyoss.s2.common.hash.Hash;

import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.object.canon.NamedUserIdObject;
import com.symphony.oss.models.object.canon.UserIdObject;

/**
 * Request to fetch a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchPartitionRequest
{
  private final Hash         partitionHash_;
  
  /**
   * Constructor.
   */
  FetchPartitionRequest(AbstractBuilder<?,?> builder)
  {
    partitionHash_ = builder.hash_;
  }
  /**
   * 
   * @return The hash of the required Partition.
   */
  public Hash getPartitionHash()
  {
    return partitionHash_;
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, FetchPartitionRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchPartitionRequest construct()
    {
      return new FetchPartitionRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchPartitionRequest> extends BaseAbstractBuilder<T,B>
  {
    protected Hash            hash_;
    protected UserIdObject    id_;
    protected String          name_;
    protected PodAndUserId    owner_;
    
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
     * Set the owner of the partition.
     * 
     * @param owner The content type for the sequence.
     * 
     * @return This (fluent method)
     */
    public T withOwner(PodAndUserId owner)
    {
      owner_ = owner;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkValueCount("Hash, ID and Name", 1, 1, hash_, id_, name_);
      faultAccumulator.checkValueCount("Hash, ID and Owner", 1, 1, hash_, id_, owner_);
      faultAccumulator.checkValueCount("Hash and Owner", 1, 1, hash_, owner_);
      faultAccumulator.checkValueCount("ID and Owner", 1, 1, id_, owner_);
      
      if(hash_ == null)
      {
        if(id_ == null)
        {
          hash_ =  new NamedUserIdObject.Builder()
              .withName(name_)
              .withUserId(owner_)
              .build()
              .getHash();
        }
        else
        {
          hash_ = id_.getHash();
        }
      }
    }
  }
}
