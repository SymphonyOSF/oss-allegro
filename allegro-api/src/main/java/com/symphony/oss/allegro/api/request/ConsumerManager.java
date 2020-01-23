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

import org.symphonyoss.s2.fugue.pipeline.IConsumer;
import org.symphonyoss.s2.fugue.pipeline.IErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.IRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.ISimpleErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.ISimpleRetryableConsumer;

/**
 * Single Threaded Manager of Consumers.
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
 * IStoredApplicationObject
 * ISystemObject
 * Object
 * </pre>
 * 
 * In fact, when reading from the object store, the object being consumed will always be an instance of
 * IStoredApplicationObject and when reading from a feed the object will always be an instance of IEnvelope
 * so registering a consumer of those types represents a "catch all", but it is also possible to
 * register a handler of type Object if this is preferred.
 * 
 * @author Bruce Skingle
 */
public class ConsumerManager extends AbstractConsumerManager
{
  ConsumerManager(AbstractBuilder<?,?> builder)
  {
    super(builder);
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
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, ConsumerManager>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected ConsumerManager construct()
    {
      return new ConsumerManager(this);
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
    @Override
    public <C> Builder withConsumer(Class<C> type, IRetryableConsumer<C> consumer)
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
    public Builder withConsumer(AbstractAdaptor<?> adaptor)
    {
      return super.withConsumerAdaptor(adaptor);
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
    @Override
    public <C> Builder withConsumer(Class<C> type, ISimpleRetryableConsumer<C> consumer)
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
    @Override
    public Builder withDefaultConsumer(ISimpleRetryableConsumer<Object> defaultConsumer)
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
    @Override
    public Builder withDefaultConsumer(IConsumer<Object> defaultConsumer)
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
    public Builder withUnprocessableMessageConsumer(IErrorConsumer<Object> unprocessableMessageConsumer)
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
    public Builder withUnprocessableMessageConsumer(ISimpleErrorConsumer<Object> unprocessableMessageConsumer)
    {
      return super.withUnprocessableMessageConsumer(unprocessableMessageConsumer);
    }
  }
}
