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

/**
 * Request to fetch a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchPartitionUsersRequest extends NamedUserIdObjectOrHashRequest
{
  /**
   * Constructor.
   */
  FetchPartitionUsersRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, FetchPartitionUsersRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchPartitionUsersRequest construct()
    {
      return new FetchPartitionUsersRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchPartitionUsersRequest> extends NamedUserIdObjectOrHashRequest.AbstractBuilder<T,B>
  {
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
  }
}
