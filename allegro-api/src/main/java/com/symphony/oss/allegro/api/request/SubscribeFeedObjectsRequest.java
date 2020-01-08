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

/**
 * Request to fetch a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class SubscribeFeedObjectsRequest extends NamedUserIdObjectOrHashRequest
{
  private final ThreadSafeConsumerManager consumerManager_;
  private final int                       subscriberThreadPoolSize_;
  private final int                       handlerThreadPoolSize_;

  /**
   * Constructor.
   */
  SubscribeFeedObjectsRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);

    consumerManager_  = builder.consumerManager_;
    subscriberThreadPoolSize_       = builder.subscriberThreadPoolSize_;
    handlerThreadPoolSize_          = builder.handlerThreadPoolSize_;
  }
  
  /**
   * 
   * @return The ConsumerManager to receive objects.
   */
  public ThreadSafeConsumerManager getConsumerManager()
  {
    return consumerManager_;
  }

  /**
   * 
   * @return The size of the subscriber thread pool.
   */
  public int getSubscriberThreadPoolSize()
  {
    return subscriberThreadPoolSize_;
  }

  /**
   * 
   * @return The size of the handler thread pool.
   */
  public int getHandlerThreadPoolSize()
  {
    return handlerThreadPoolSize_;
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, SubscribeFeedObjectsRequest>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected SubscribeFeedObjectsRequest construct()
    {
      return new SubscribeFeedObjectsRequest(this);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends SubscribeFeedObjectsRequest> extends NamedUserIdObjectOrHashRequest.AbstractBuilder<T,B>
  {
    protected ThreadSafeConsumerManager consumerManager_;
    protected int                       subscriberThreadPoolSize_ = 1;
    protected int                       handlerThreadPoolSize_    = 1;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the ConsumerManager to receive objects.
     * 
     * @param consumerManager The ConsumerManager to receive objects.
     * 
     * @return This (fluent method)
     */
    public T withConsumerManager(ThreadSafeConsumerManager consumerManager)
    {
      consumerManager_ = consumerManager;
      
      return self();
    }

    /**
     * Set the size of the thread pool for subscriber requests.
     * 
     * @param subscriberThreadPoolSize The size of the thread pool for subscriber requests.
     * 
     * The subscriber thread pool is used to make connections over the network to request a batch
     * of messages. Once a batch is received, all but one of the messages in the batch are passed
     * individually to the handler thread pool and the final one is processed in the subscriber thread.
     * 
     * @return This (fluent method)
     */
    public T withSubscriberThreadPoolSize(int subscriberThreadPoolSize)
    {
      subscriberThreadPoolSize_ = subscriberThreadPoolSize;
      
      return self();
    }

    /**
     * Set the size of the thread pool for handler requests.
     * 
     * @param handlerThreadPoolSize The size of the thread pool for handler requests.
     * 
     * The handler thread pool is used to process messages received in a batch in parallel.
     * The optimum size of the handler thread pool is 9 * subscriberThreadPoolSize.
     * 
     * @return This (fluent method)
     */
    public T withHandlerThreadPoolSize(int handlerThreadPoolSize)
    {
      handlerThreadPoolSize_ = handlerThreadPoolSize;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      // Maybe this should be an error, but for now we'll just create a consumer manager with just the default print to stdout consumer.
      if(consumerManager_ == null)
        consumerManager_ = new ThreadSafeConsumerManager.Builder().build();
      
      if(subscriberThreadPoolSize_ < 1)
        faultAccumulator.error("SubscriberThreadPoolSize must be at least 1.");
      
      if(handlerThreadPoolSize_ < 1)
        faultAccumulator.error("HandlerThreadPoolSize must be at least 1.");
    }
  }
}
