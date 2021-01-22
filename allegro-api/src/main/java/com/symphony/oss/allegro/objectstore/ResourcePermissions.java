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

package com.symphony.oss.allegro.objectstore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;

/**
 * A set of permissions applied to a resource.
 * 
 * @author Bruce Skingle
 *
 */
public class ResourcePermissions
{
  private final ImmutableMap<PodAndUserId, Set<Permission>> userPermissions_;
  
  /**
   * Constructor.
   */
  ResourcePermissions(AbstractBuilder<?,?> builder)
  {
    Map<PodAndUserId, ImmutableSet<Permission>> immutableSetMap = new HashMap<>();
    
    for(Entry<PodAndUserId, Set<Permission>> entry : builder.userPermissions_.entrySet())
    {
      immutableSetMap.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
    }
    
    userPermissions_ = ImmutableMap.copyOf(immutableSetMap);
  }

  /**
   * 
   * @return The user permissions.
   */
  public ImmutableMap<PodAndUserId, Set<Permission>> getUserPermissions()
  {
    return userPermissions_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, ResourcePermissions>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected ResourcePermissions construct()
    {
      return new ResourcePermissions(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends ResourcePermissions> extends BaseAbstractBuilder<T,B>
  {
    protected Map<PodAndUserId, Set<Permission>> userPermissions_ = new HashMap<>();
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Add the given user ID to the set of users who are allowed access to the resource.
     * 
     * @param userId      The ID of a user 
     * @param permissions The permissions the given user should have
     * 
     * @return This (fluent method)
     */
    public T withUser(PodAndUserId userId, Set<Permission> permissions)
    {
      userPermissions_.put(userId, permissions);
      
      return self();
    }
    
    /**
     * Add the given user ID to the set of users who are allowed access to the resource.
     * 
     * @param userId      The ID of a user 
     * @param permissions The permissions the given user should have
     * 
     * @return This (fluent method)
     */
    public T withUser(PodAndUserId userId, Permission...permissions)
    {
      Set<Permission> set = new HashSet<>();
      
      for(Permission permission : permissions)
        set.add(permission);
      
      userPermissions_.put(userId, set);
      
      return self();
    }
  }
}
