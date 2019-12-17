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
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeSimpleErrorConsumer;

import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;

/**
 * Request to fetch a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class SubscribeFeedObjectsRequest extends FeedObjectsRequest
{
  private final IThreadSafeErrorConsumer<IAbstractStoredApplicationObject> unprocessableMessageConsumer_;
  private final int                                                        subscriberThreadPoolSize_;
  private final int                                                        handlerThreadPoolSize_;
  
  /**
   * Constructor.
   */
  SubscribeFeedObjectsRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);

    unprocessableMessageConsumer_   = builder.unprocessableMessageConsumer_;
    subscriberThreadPoolSize_       = builder.subscriberThreadPoolSize_;
    handlerThreadPoolSize_          = builder.handlerThreadPoolSize_;
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
   * 
   * @return The consumer to which unprocessable messages will be directed.
   */
  public IThreadSafeErrorConsumer<IAbstractStoredApplicationObject> getUnprocessableMessageConsumer()
  {
    return unprocessableMessageConsumer_;
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends SubscribeFeedObjectsRequest> extends FeedObjectsRequest.AbstractBuilder<T,B>
  {
    private IThreadSafeErrorConsumer<IAbstractStoredApplicationObject> unprocessableMessageConsumer_;
    private int                                                        subscriberThreadPoolSize_ = 1;
    private int                                                        handlerThreadPoolSize_    = 1;
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
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

    /**
     * Set the consumer to which unprocessable messages will be directed.
     * 
     * @param unprocessableMessageConsumer The consumer to which unprocessable messages will be directed.
     * 
     * @return This (fluent method)
     */
    public T withUnprocessableMessageConsumer(IThreadSafeErrorConsumer<IAbstractStoredApplicationObject> unprocessableMessageConsumer)
    {
      unprocessableMessageConsumer_ = unprocessableMessageConsumer;
      
      return self();
    }

    /**
     * Set the consumer to which unprocessable messages will be directed.
     * 
     * @param unprocessableMessageConsumer The consumer to which unprocessable messages will be directed.
     * 
     * This convenience method accepts a non-closable consumer, which is a functional interface and is
     * convenient to use in cases where a close notification is not required.
     * 
     * @return This (fluent method)
     */
    public T withUnprocessableMessageConsumer(IThreadSafeSimpleErrorConsumer<IAbstractStoredApplicationObject> unprocessableMessageConsumer)
    {
      unprocessableMessageConsumer_ = new IThreadSafeErrorConsumer<IAbstractStoredApplicationObject>()
          {

            @Override
            public void consume(IAbstractStoredApplicationObject item, ITraceContext trace, String message, Throwable cause)
            {
              unprocessableMessageConsumer.consume(item, trace, message, cause);
            }

            @Override
            public void close(){}
          };
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(unprocessableMessageConsumer_, "UnprocessableMessageConsumer must not be set to null (there is a default, you don't have to set one)");
      
      if(subscriberThreadPoolSize_ < 1)
        faultAccumulator.error("SubscriberThreadPoolSize must be at least 1.");
      
      if(handlerThreadPoolSize_ < 1)
        faultAccumulator.error("HandlerThreadPoolSize must be at least 1.");
    }
  }
}
