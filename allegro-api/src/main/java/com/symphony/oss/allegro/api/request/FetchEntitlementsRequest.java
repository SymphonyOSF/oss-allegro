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

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.object.canon.NamedUserIdObject;

/**
 * Request for some set of entitlements for a particular user.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchEntitlementsRequest
{
  private final PodAndUserId       userId_;
  private final ImmutableSet<Hash> entitlementHashes_;
  
  /**
   * Constructor.
   */
  FetchEntitlementsRequest(AbstractBuilder<?,?> builder)
  {
    userId_             = builder.userId_;
    entitlementHashes_  = ImmutableSet.copyOf(builder.entitlementHashes_);
  }

  /**
   * 
   * @return The ID of the user for whom entitlements are requested.
   */
  public PodAndUserId getUserId()
  {
    return userId_;
  }

  /**
   * 
   * @return The set of entitlements required.
   */
  public ImmutableSet<Hash> getEntitlementHashes()
  {
    return entitlementHashes_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, FetchEntitlementsRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchEntitlementsRequest construct()
    {
      return new FetchEntitlementsRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchEntitlementsRequest> extends BaseAbstractBuilder<T,B>
  {
    protected PodAndUserId userId_;
    protected Set<Hash>    entitlementHashes_ = new HashSet<>();
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the user ID of the user for whom entitlements are being requested.
     * 
     * @param userId The user ID of the user for whom entitlements are being requested.
     * 
     * @return This (fluent method)
     */
    public T withUserId(PodAndUserId userId)
    {
      userId_ = userId;
      
      return self();
    }
    
    /**
     * Add the given hash to the set of entitlements to be returned.
     * 
     * @param hash The hash of the required entitlement.
     * 
     * @return This (fluent method)
     */
    public T withEntitlementHash(Hash hash)
    {
      entitlementHashes_.add(hash);
      
      return self();
    }
    
    /**
     * Add the given entitlement to the set of entitlements to be returned.
     * 
     * @param owner The entitlement owner.
     * @param name  The entitlement name.
     * 
     * @return This (fluent method)
     */
    public T withEntitlementHash(PodAndUserId owner, String name)
    {
      entitlementHashes_.add(new NamedUserIdObject.Builder()
          .withUserId(owner)
          .withName(name)
          .build()
          .getHash());
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkNotNull(userId_, "User ID");
      if(entitlementHashes_.isEmpty())
        faultAccumulator.error("At least one entitlement must be specified.");

    }
  }
}
