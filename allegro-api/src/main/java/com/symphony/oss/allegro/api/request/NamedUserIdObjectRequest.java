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

import org.symphonyoss.s2.canon.runtime.exception.BadRequestException;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;

import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.object.canon.INamedUserIdObject;
import com.symphony.oss.models.object.canon.NamedUserIdObject;

/**
 * Base object for a Request relating to an object identified by an INamedUserIdObject (e.g. a Partition or Feed.),
 * where the ID object is required (e.g. for upsert).
 * 
 * @author Bruce Skingle
 *
 */
public class NamedUserIdObjectRequest
{
  private static final String CALLER_MUST_BE_OWNER = "The provided ID object MUST contain the callers userId";
  
  private final INamedUserIdObject id_;
  private final String             name_;
  private final PodAndUserId       owner_;
  
  /**
   * Constructor.
   */
  NamedUserIdObjectRequest(AbstractBuilder<?,?> builder)
  {
    id_   = builder.id_;
    name_ = builder.name_;
    owner_ = builder.owner_;
  }
  
  /**
   * Return the ID of the required object.
   * 
   * @param defaultOwner The default value for the owner element of the ID.
   * 
   * @return The ID of the required object.
   */
  public INamedUserIdObject getId(PodAndUserId defaultOwner)
  {
    if(id_ == null)
    {
      return new NamedUserIdObject.Builder()
        .withName(name_)
        .withUserId(owner_ == null ? defaultOwner : owner_)
        .build()
        ;
    }
    else
    {
      return id_;
    }
  }
  
  /**
   * Return the ID of the required object, ensuring that the owner is the one given.
   * 
   * @param requiredOwner The required value for the owner element of the ID.
   * 
   * @return The ID of the required object.
   * 
   * @throws BadRequestException if the owner is not the required value.
   */
  public INamedUserIdObject getAndValidateId(PodAndUserId requiredOwner)
  {
    if(id_ == null)
    {
      if(owner_!= null && !requiredOwner.equals(owner_))
        throw new BadRequestException(CALLER_MUST_BE_OWNER);
      
      return new NamedUserIdObject.Builder()
        .withName(name_)
        .withUserId(requiredOwner)
        .build()
        ;
    }
    else
    {
      if(!requiredOwner.equals(id_.getUserId()))
        throw new BadRequestException(CALLER_MUST_BE_OWNER);
      
      return id_;
    }
  }

// There is no need to instantiate this directly
//  /**
//   * Builder.
//   * 
//   * @author Bruce Skingle
//   *
//   */
//  public static class Builder extends AbstractBuilder<Builder, UserIdObjectRequest>
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
//    protected UserIdObjectRequest construct()
//    {
//      return new UserIdObjectRequest(this);
//    }
//  }

  /**
   * AbstractBuilder.
   * 
   * Only one of
   * <ul> 
   * <li> Name (and optionally Owner)
   * <li> ID
   * </ul>
   * 
   * may be set, if this is not the case (e.g. if you set ID and Name) then an exception will be thrown from the build()
   * method.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends NamedUserIdObjectRequest> extends BaseAbstractBuilder<T,B>
  {
    protected INamedUserIdObject    id_;
    protected String                name_;
    protected PodAndUserId          owner_;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the name element of the ID object.
     * This method is only appropriate if the ID is a simple UserIdObject, if a subclass of this type has been used
     * so that additional attributes can be included in the ID then you must call withHash() or withId() instead.
     * 
     * @param name The name element of the ID object.
     * 
     * @return This (fluent method)
     */
    public T withName(String name)
    {
      name_ = name;
      
      return self();
    }
    
    /**
     * Set the owner element of the ID object.
     * This method is only appropriate if the ID is a simple UserIdObject, if a subclass of this type has been used
     * so that additional attributes can be included in the ID then you must call withHash() or withId() instead.
     * 
     * @param owner The owner element of the ID object.
     * 
     * @return This (fluent method)
     */
    public T withOwner(PodAndUserId owner)
    {
      owner_ = owner;
      
      return self();
    }
    
    /**
     * Set the ID object.
     * 
     * @param id The ID object.
     * 
     * @return This (fluent method)
     */
    public T withId(INamedUserIdObject id)
    {
      id_ = id;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkValueCount("ID and Name", 1, 1, id_, name_);
      faultAccumulator.checkValueCount("ID and Owner", 0, 1, id_, owner_);
    }
  }
}
