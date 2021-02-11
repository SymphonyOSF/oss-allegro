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

import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.internal.pod.canon.AckId;
import com.symphony.oss.models.internal.pod.canon.FeedId;

/**
 * A request object for message feed requests.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchFeedMessagesRequest
{
  private final FeedId                  feedId_;
  private final AckId                   ackId_;
  private final AllegroConsumerManager consumerManager_;
  
  protected FetchFeedMessagesRequest(AbstractBuilder<?,?> builder)
  {
    feedId_ = builder.feedId_;
    ackId_ = builder.ackId_;
    consumerManager_  = builder.consumerManager_;
  }

  /**
   * 
   * @return The thread ID.
   */
  public FeedId getFeedId()
  {
    return feedId_;
  }

  /**
   * 
   * @return The thread ID.
   */
  public AckId getAckId()
  {
    return ackId_;
  }

  /**
   * 
   * @return The ConsumerManager to receive objects.
   */
  public AllegroConsumerManager getConsumerManager()
  {
    return consumerManager_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, FetchFeedMessagesRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected FetchFeedMessagesRequest construct()
    {
      return new FetchFeedMessagesRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchFeedMessagesRequest> extends BaseAbstractBuilder<T,B>
  {
    private FeedId                  feedId_;
    private AckId                   ackId_;
    private AllegroConsumerManager consumerManager_;
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the feedId.
     * 
     * @param feedId The feedId.
     * 
     * @return This (fluent method)
     */
    public T withFeedId(FeedId feedId)
    {
      feedId_ = feedId;
      
      return self();
    }
    
    /**
     * Set the ackId.
     * 
     * @param ackId The ackId.
     * 
     * @return This (fluent method)
     */
    public T withAckId(AckId ackId)
    {
      ackId_ = ackId;
      
      return self();
    }
    
    /**
     * Set the ConsumerManager to receive objects.
     * 
     * @param consumerManager The ConsumerManager to receive objects.
     * 
     * @return This (fluent method)
     */
    public T withConsumerManager(AllegroConsumerManager consumerManager)
    {
      consumerManager_ = consumerManager;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(feedId_, "Feed ID");
      faultAccumulator.checkNotNull(consumerManager_, "Consumer Manager");
    }
  }
}
