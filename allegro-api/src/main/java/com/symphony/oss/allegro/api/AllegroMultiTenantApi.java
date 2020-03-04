/*
 *
 *
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

package com.symphony.oss.allegro.api;

import org.symphonyoss.s2.common.fault.FaultAccumulator;

import com.symphony.oss.models.core.canon.facade.PodAndUserId;

public class AllegroMultiTenantApi extends AllegroBaseApi implements IAllegroMultiTenantApi
{
  final PodAndUserId                    userId_;
  
  AllegroMultiTenantApi(AbstractBuilder<?, ?> builder)
  {
    super(builder);
    
    userId_ = builder.userId_;
  }
  
  public static class Builder extends AbstractBuilder<Builder, IAllegroMultiTenantApi>
  {

    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected IAllegroMultiTenantApi construct()
    {
      return new AllegroMultiTenantApi(this);
    }
    
  }
  
  /**
   * The builder implementation.
   * 
   * This is implemented as an abstract class to allow for sub-classing in future.
   * 
   * Any sub-class of AllegroApi would need to implement its own Abstract sub-class of this class
   * and then a concrete Builder class which is itself a sub-class of that.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The type of the concrete Builder
   * @param <B> The type of the built class, some subclass of AllegroApi
   */
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends IAllegroMultiTenantApi>
  extends AllegroBaseApi.AbstractBuilder<T, B>
  {
    protected PodAndUserId                    userId_;
    
    public AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    public T withUserId(PodAndUserId userId)
    {
      userId_ = userId;
      
      return self();
    }
    
    public T withUserId(long userId)
    {
      userId_ = PodAndUserId.newBuilder().build(userId);
      
      return self();
    }

    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(userId_, "User ID");
    }
  }

  @Override
  public PodAndUserId getUserId()
  {
    return userId_;
  }

  @Override
  public String getSessionToken()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
