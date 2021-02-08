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

package com.symphony.oss.allegro2.api;

import java.util.EnumSet;
import java.util.Objects;

import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.pod.canon.StreamTypeEnum;

/**
 * A request object for fetch stream requests.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchStreamsRequest
{
  private final long skip_;
  private final long limit_;
  private final boolean inactive_;
  private final EnumSet<StreamTypeEnum> streamTypes_;
  
  FetchStreamsRequest(AbstractBuilder<?,?> builder)
  {
    skip_ = builder.skip_;
    limit_ = builder.limit_;
    inactive_ = builder.inactive_;
    streamTypes_ = builder.streamTypes_;
  }

  /**
   * Return the number of results which should be skipped.
   * 
   * @return the number of results which should be skipped.
   */
  public long getSkip()
  {
    return skip_;
  }

  /**
   * Return the maximum number of streams to return.
   * 
   * @return the maximum number of streams to return.
   */
  public long getLimit()
  {
    return limit_;
  }

  /**
   * Return the maximum number of streams to return.
   * 
   * @return the maximum number of streams to return.
   */
  public boolean isInactive()
  {
    return inactive_;
  }

  /**
   * Return the stream types which should be included in the response.
   * 
   * @return the stream types which should be included in the response.
   */
  public EnumSet<StreamTypeEnum> getStreamTypes()
  {
    return streamTypes_;
  }

  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchStreamsRequest> extends BaseAbstractBuilder<T,B>
  {
    private long                    skip_        = 0L;
    private long                    limit_       = 0L;
    private boolean                 inactive_    = false;
    private EnumSet<StreamTypeEnum> streamTypes_ = EnumSet.noneOf(StreamTypeEnum.class);
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the number of results which should be skipped.
     * 
     * @param skip The number of results which should be skipped.
     * 
     * @return This (fluent method)
     */
    public T withSkip(long skip)
    {
      skip_ = skip;
      
      return self();
    }

    /**
     * Set the maximum number of streams to return.
     * 
     * @param limit the maximum number of streams to return.
     * 
     * @return This (fluent method)
     */
    public T withLimit(long limit)
    {
      limit_ = limit;
      
      return self();
    }

    /**
     * Set the maximum number of streams to return.
     * 
     * @param inactive Whether inactive streams should be included in the result set.
     * 
     * @return This (fluent method)
     */
    public T withInactive(boolean inactive)
    {
      inactive_ = inactive;
      
      return self();
    }

    /**
     * Add the given stream type to those which will be returned.
     * 
     * By default, all stream types will be returned.
     * 
     * @param streamType A stream type which should be included in the response.
     * 
     * @return This (fluent method)
     */
    public T withStreamType(StreamTypeEnum streamType)
    {
      streamTypes_.add(streamType);
      
      return self();
    }

    /**
     * Set the stream types which should be included in the response.
     * 
     * If the set is empty then all stream types will be returned.
     * 
     * @param streamTypes The stream types which should be included in the response.
     * 
     * @return This (fluent method)
     */
    public T withStreamTypes(EnumSet<StreamTypeEnum> streamTypes)
    {
      Objects.nonNull(streamTypes);
      
      streamTypes_ = streamTypes;
      
      return self();
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, FetchStreamsRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchStreamsRequest construct()
    {
      return new FetchStreamsRequest(this);
    }
  }
}
