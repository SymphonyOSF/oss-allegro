/*
 *
 *
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

import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.fugue.pipeline.ISimpleThreadSafeRetryableConsumer;
import com.symphony.oss.fugue.pipeline.IThreadSafeErrorConsumer;
import com.symphony.oss.fugue.pipeline.IThreadSafeRetryableConsumer;
import com.symphony.oss.fugue.pipeline.IThreadSafeSimpleErrorConsumer;
import com.symphony.oss.fugue.trace.ITraceContext;

/**
 * Manager of Thread Safe Consumers.
 * 
 * When the consume method is called the consumer with the most specific type to the object
 * being consumed will be selected. Objects will be unwrapped if necessary to obtain a more
 * specific type to match with a consumer.
 * 
 * Sub types of the following types are considered more specific than the ones which follow:
 * <pre>
 * IChatMessage
 * IApplicationObject
 * </pre>
 * 
 * For example, if a StoredApplicationObject containing a SocialMessage was consumed, then consumers
 * would be selected in the following order of precedence:
 *
 * <pre>
 * IReceivedChatMessage
 * IChatMessage
 * ISocialMessage
 * ILiveCurrentMessage
 * IApplicationObject
 * IAbstractStoredApplicationObject
 * IStoredApplicationObject
 * ISystemObject
 * Object
 * </pre>
 * 
 * In fact, when reading from the object store, the object being consumed will always be an instance of
 * IStoredApplicationObject or IDeletedApplicationObject and these are both sub-interfaces of IAbstractStoredApplicationObject,
 * so registering a consumer of those types represents a "catch all", but it is also possible to
 * register a handler of type Object if this is preferred.
 * 
 * When reading chat messages, the object being consumed will always be an instance of ILiveCurrentMessage.
 * 
 * @author Bruce Skingle
 */
public class AsyncConsumerManager extends AbstractConsumerManager
{
  //private static final Logger log_ = LoggerFactory.getLogger(AsyncConsumerManager.class);
  
  private final Integer                    subscriberThreadPoolSize_;
  private final Integer                    handlerThreadPoolSize_;
  
  protected AsyncConsumerManager(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    subscriberThreadPoolSize_       = builder.subscriberThreadPoolSize_;
    handlerThreadPoolSize_          = builder.handlerThreadPoolSize_;
  }

  /**
   * 
   * @return The size of the subscriber thread pool.
   */
  public Integer getSubscriberThreadPoolSize()
  {
    return subscriberThreadPoolSize_;
  }

  /**
   * 
   * @return The size of the handler thread pool.
   */
  public Integer getHandlerThreadPoolSize()
  {
    return handlerThreadPoolSize_;
  }
  
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractConsumerManager> extends AbstractConsumerManager.AbstractBuilder<T,B>
  {
    protected Integer                              subscriberThreadPoolSize_;
    protected Integer                              handlerThreadPoolSize_;

    AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    /**
     * Set the consumer to which unprocessable messages will be directed.
     * 
     * @param unprocessableMessageConsumer The consumer to which unprocessable messages will be directed.
     * 
     * @return This (fluent method)
     */
    protected T withUnprocessableMessageConsumer(IThreadSafeErrorConsumer<Object> unprocessableMessageConsumer)
    {
      return super.withUnprocessableMessageConsumer(unprocessableMessageConsumer);
    }

    /**
     * Set the consumer to which unprocessable messages will be directed.
     * 
     * @param unprocessableMessageConsumer The consumer to which unprocessable messages will be directed.
     * 
     * @return This (fluent method)
     */
    protected T withUnprocessableMessageConsumer(IThreadSafeSimpleErrorConsumer<Object> unprocessableMessageConsumer)
    {
      return super.withUnprocessableMessageConsumer(new IThreadSafeErrorConsumer<Object>()
      {

        @Override
        public void consume(Object item, ITraceContext trace, String message, Throwable cause)
        {
          unprocessableMessageConsumer.consume(item, trace, message, cause);
        }

        @Override
        public void close(){}
      });
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
    public T withSubscriberThreadPoolSize(Integer subscriberThreadPoolSize)
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
    public T withHandlerThreadPoolSize(Integer handlerThreadPoolSize)
    {
      handlerThreadPoolSize_ = handlerThreadPoolSize;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      if(subscriberThreadPoolSize_!=null && subscriberThreadPoolSize_ < 1)
        faultAccumulator.error("SubscriberThreadPoolSize must be at least 1 or not set.");
      
      if(handlerThreadPoolSize_!=null && handlerThreadPoolSize_ < 1)
        faultAccumulator.error("HandlerThreadPoolSize must be at least 1 or not set.");
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, AsyncConsumerManager>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    public AsyncConsumerManager construct()
    {
      return new AsyncConsumerManager(this);
    }

    /**
     * Add the given consumer of the given payload type.
     * 
     * @param <C>       The type of the payload this consumer accepts.
     * @param type      The type of the payload this consumer accepts.
     * @param consumer  A consumer of the given type.
     * 
     * The <code>close()</code> method of all consumers will be called when the request completes.
     * 
     * @return This (fluent method).
     */
    public <C> Builder withConsumer(Class<C> type, IThreadSafeRetryableConsumer<C> consumer)
    {
      return super.withConsumer(type, consumer);
    }

    /**
     * Add the given adaptor as a consumer.
     * 
     * In general, an adaptor will be a consumer for a super-class of some set of types for which it
     * adapts. It is possible to provide an Adaptor as well as specific Consumers, in this case the
     * Consumers will normally be called in preference to the adaptor since they register for more
     * specific types.
     * 
     * @param adaptor An adaptor.
     * 
     * @return This (fluent method).
     */
    public Builder withConsumer(ThreadSafeAbstractAdaptor<?> adaptor)
    {
      return withConsumerAdaptor(adaptor);
    }
    
    @SuppressWarnings("unchecked")
    private Builder withConsumerAdaptor(@SuppressWarnings("rawtypes") ThreadSafeAbstractAdaptor adaptor)
    {
      adaptor.setDefaultConsumer((IThreadSafeRetryableConsumer<Object>) getDefaultConsumer());
      
      return withConsumer(adaptor.getPayloadType(), adaptor);
    }

    /**
     * Add the given consumer of the given payload type.
     * 
     * @param <C>       The type of the payload this consumer accepts.
     * @param type      The type of the payload this consumer accepts.
     * @param consumer  A consumer of the given type.
     * 
     * This method accepts a simple consumer, which is a functional interface and is convenient
     * to use in cases where a close notification is not required.
     * 
     * @return This (fluent method).
     */
    public <C> Builder withConsumer(Class<C> type, ISimpleThreadSafeRetryableConsumer<C> consumer)
    {
      return super.withConsumer(type, consumer);
    }

    /**
     * Set the consumer to be called with objects for which there is no match with any other consumer.
     * 
     * @param defaultConsumer  The consumer to be called with objects for which there is no match with any other consumer.
     * 
     * The default implementation (the default default) simply logs messages it consumes.
     * 
     * This method accepts a simple consumer, which is a functional interface and is convenient
     * to use in cases where a close notification is not required.
     * 
     * @return This (fluent method).
     */
    public Builder withDefaultConsumer(ISimpleThreadSafeRetryableConsumer<Object> defaultConsumer)
    {
      return super.withDefaultConsumer(defaultConsumer);
    }

    /**
     * Add the consumer to be called with objects for which there is no match with any other consumer.
     * 
     * @param defaultConsumer  The consumer to be called with objects for which there is no match with any other consumer.
     * 
     * The default implementation (the default default) simply logs messages it consumes.
     * 
     * The <code>close()</code> method of all consumers will be called when the request completes.
     * 
     * @return This (fluent method).
     */
    public Builder withDefaultConsumer(IThreadSafeRetryableConsumer<Object> defaultConsumer)
    {
      return super.withDefaultConsumer(defaultConsumer);
    }
    
    /**
     * Set the consumer to which unprocessable messages will be directed.
     * 
     * @param unprocessableMessageConsumer The consumer to which unprocessable messages will be directed.
     * 
     * @return This (fluent method)
     */
    @Override
    public Builder withUnprocessableMessageConsumer(IThreadSafeErrorConsumer<Object> unprocessableMessageConsumer)
    {
      return super.withUnprocessableMessageConsumer(unprocessableMessageConsumer);
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
    @Override
    public Builder withUnprocessableMessageConsumer(IThreadSafeSimpleErrorConsumer<Object> unprocessableMessageConsumer)
    {
      return super.withUnprocessableMessageConsumer(unprocessableMessageConsumer);
    }
  }
}
