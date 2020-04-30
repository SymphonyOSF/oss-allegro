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

import com.symphony.oss.allegro.api.ResourcePermissions;
import com.symphony.oss.commons.fault.FaultAccumulator;

/**
 * Request to create a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class UpsertPartitionRequest extends NamedUserIdObjectRequest
{
  private final ResourcePermissions                          permissions_;
  
  UpsertPartitionRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    permissions_   = builder.permissions_;
  }
  
  /**
   * 
   * @return The ResourcePermissions for the feed.
   */
  public ResourcePermissions getPermissions()
  {
    return permissions_;
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
    protected ResourcePermissions permissions_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    /**
     * Set the given ResourcePermissions for the feed.
     * 
     * The Feed owner can always read the feed.
     * 
     * Applicable Permissions are:
     * 
     * None
     * Read
     * Write
     * 
     * @param permissions ResourcePermissions.
     * 
     * @return This (fluent method)
     */
    public T withPermissions(ResourcePermissions permissions)
    {
      permissions_ = permissions;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
    }
  }
}
