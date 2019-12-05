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
import org.symphonyoss.s2.fugue.pipeline.ISimpleThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;

/**
 * Manager of Thread Safe Consumers.
 * 
 * @author Bruce Skingle
 */
public class ThreadSafeConsumerManager extends ConsumerManager
{
  private static final Logger log_ = LoggerFactory.getLogger(ThreadSafeConsumerManager.class);
  
  protected ThreadSafeConsumerManager(AbstractBuilder<?,?> builder)
  {
    super(builder);
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
    public <C> Builder withConsumer(Class<C> type, IThreadSafeConsumer<C> consumer)
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
      return withThreadSafeConsumerAdaptor(adaptor);
    }
    
    @SuppressWarnings("unchecked")
    public Builder withThreadSafeConsumerAdaptor(@SuppressWarnings("rawtypes") ThreadSafeAbstractAdaptor adaptor)
    {
      adaptor.setDefaultConsumer((IThreadSafeConsumer<Object>) getDefaultConsumer());
      
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
    public <C> Builder withConsumer(Class<C> type, ISimpleThreadSafeConsumer<C> consumer)
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
    public Builder withDefaultConsumer(ISimpleThreadSafeConsumer<Object> defaultConsumer)
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
    public Builder withDefaultConsumer(IThreadSafeConsumer<Object> defaultConsumer)
    {
      return super.withDefaultConsumer(defaultConsumer);
    }
  }
}
