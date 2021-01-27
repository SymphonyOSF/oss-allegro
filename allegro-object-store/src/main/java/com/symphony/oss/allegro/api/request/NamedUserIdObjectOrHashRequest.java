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

import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.object.canon.NamedUserIdObject;

/**
 * Base object for a Request relating to an object identified by an INamedUserIdObject (e.g. a Partition or Feed.),
 * where only the hash of the ID object is required (e.g. for fetch).
 * 
 * The ID object or a name and optional owner can be provided to the builder as a convenience.
 * 
 * @author Bruce Skingle
 *
 */
public class NamedUserIdObjectOrHashRequest extends NamedUserIdObjectRequest
{
  private final Hash         hash_;
  
  /**
   * Constructor.
   */
  NamedUserIdObjectOrHashRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    hash_ = builder.hash_;
  }
  
  /**
   * Return the hash of the required object.
   * 
   * @param defaultOwner The default value for the owner element of the ID.
   * 
   * @return The hash of the required object.
   */
  public Hash getHash(PodAndUserId defaultOwner)
  {
    if(hash_ == null)
    {
      return getId(defaultOwner).getHash();
    }
    else
    {
      return hash_;
    }
  }

//  /**
//   * Builder.
//   * 
//   * @author Bruce Skingle
//   *
//   */
//  public static class Builder extends AbstractBuilder<Builder, UserIdObjectOrHashRequest>
//  {
//    /**
//     * Constructor.
//     */
//    public Builder()
//    {
//      super(Builder.class);
//    }
//
//    @Override
//    protected UserIdObjectOrHashRequest construct()
//    {
//      return new UserIdObjectOrHashRequest(this);
//    }
//  }

  /**
   * AbstractBuilder.
   * 
   * Only one of
   * <ul> 
   * <li> Name (and optionally Owner)
   * <li> ID
   * <li> Hash
   * </ul>
   * 
   * may be set, if this is not the case (e.g. if you set ID and Hash) then an exception will be thrown from the build()
   * method.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends NamedUserIdObjectOrHashRequest> extends NamedUserIdObjectRequest.AbstractBuilder<T,B>
  {
    protected Hash            hash_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the hash of the ID object.
     * 
     * Only one of
     * <ul> 
     * <li> Name (and optionally Owner)
     * <li> ID
     * <li> Hash
     * </ul>
     * 
     * may be set, if this is not the case (e.g. if you set ID and Hash) then an exception will be thrown from the build()
     * method.
     * 
     * @param hash The hash of the ID object. 
     * 
     * @return This (fluent method)
     */
    public T withHash(Hash hash)
    {
      hash_ = hash;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      faultAccumulator.checkValueCount("Hash, ID and Name", 1, 1, hash_, id_, name_);
      faultAccumulator.checkValueCount("Hash, ID and Owner", 0, 1, hash_, id_, owner_);
//      faultAccumulator.checkValueCount("Hash and Owner", 1, 1, hash_, owner_);
//      faultAccumulator.checkValueCount("ID and Owner", 1, 1, id_, owner_);
//      faultAccumulator.checkValueCount("ID and Hash", 1, 1, id_, hash_);
    }
  }
}
