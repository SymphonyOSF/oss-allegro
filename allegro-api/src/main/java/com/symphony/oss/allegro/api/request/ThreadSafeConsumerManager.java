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

package com.symphony.oss.allegro.api.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.ISimpleThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeSimpleErrorConsumer;

/**
 * Manager of Thread Safe Consumers.
 * 
 * @author Bruce Skingle
 */
public class ThreadSafeConsumerManager extends ConsumerManager
{
  private static final Logger log_ = LoggerFactory.getLogger(ThreadSafeConsumerManager.class);
  
  private IThreadSafeErrorConsumer<Object> threadSafeUnprocessableMessageConsumer_;
  
  protected ThreadSafeConsumerManager(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    threadSafeUnprocessableMessageConsumer_ = builder.threadSafeUnprocessableMessageConsumer_;
  }

  /**
   * 
   * @return The consumer to which unprocessable messages will be directed.
   */
  public IThreadSafeErrorConsumer<Object> getUnprocessableMessageConsumer()
  {
    return threadSafeUnprocessableMessageConsumer_;
  }
  
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends ConsumerManager> extends ConsumerManager.AbstractBuilder<T,B>
  {
    private IThreadSafeErrorConsumer<Object> threadSafeUnprocessableMessageConsumer_;

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
      threadSafeUnprocessableMessageConsumer_ = unprocessableMessageConsumer;
      
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
      threadSafeUnprocessableMessageConsumer_ = new IThreadSafeErrorConsumer<Object>()
      {

        @Override
        public void consume(Object item, ITraceContext trace, String message, Throwable cause)
        {
          unprocessableMessageConsumer.consume(item, trace, message, cause);
        }

        @Override
        public void close(){}
      };
      
      return super.withUnprocessableMessageConsumer(threadSafeUnprocessableMessageConsumer_);
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, ThreadSafeConsumerManager>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    public ThreadSafeConsumerManager construct()
    {
      return new ThreadSafeConsumerManager(this);
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
    public Builder withConsumerAdaptor(@SuppressWarnings("rawtypes") ThreadSafeAbstractAdaptor adaptor)
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
    public Builder withUnprocessableMessageConsumer(IThreadSafeSimpleErrorConsumer<Object> unprocessableMessageConsumer)
    {
      return super.withUnprocessableMessageConsumer(unprocessableMessageConsumer);
    }
  }
}
