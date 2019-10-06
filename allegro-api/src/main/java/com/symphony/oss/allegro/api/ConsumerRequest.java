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

package com.symphony.oss.allegro.api;

import java.util.Collection;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.IConsumer;
import org.symphonyoss.s2.fugue.pipeline.ISimpleConsumer;

/**
 * Base class for request objects which take Consumers.
 *
 * @param <T> The type of payload to be consumed.
 * 
 * When the consume method is called the consumer with the most specific type to the object
 * being consumed will be selected. Objects will be unwrapped if necessary to obtain a more
 * specific type to match with a consumer.
 * 
 * Sub types of the following types are considered more specific than the ones which follow:
 * <ul>
 * <li/>IChatMessage
 * <li/>IApplicationObject
 * <li/>IFundamentalPayload
 * <li/>IFundamentalObject
 * </ul>
 * 
 * For example, if a FundamentalObject containing a SocialMessage was consumed, then consumers
 * would be selected in the following order of precedence:
 *
 * <ul>
 * <li/>IReceivedChatMessage
 * <li/>IChatMessage
 * <li/>ISocialMessage
 * <li/>ILiveCurrentMessage
 * <li/>IApplicationObject
 * <li/>IClob
 * <li/>IFundamentalPayload
 * <li/>IFundamentalObject
 * <li/>Object
 * </ul>
 * 
 * In fact, when reading from the object store, the object being consumed will always be an instance of
 * IFundamentalObject and when reading from a feed the object will always be an instance of INotification
 * so registering a consumer of those types represents a "catch all", but it is also possible to
 * register a handler of type Object if this is preferred.
 * 
 * @author Bruce Skingle
 */
public class ConsumerRequest<T extends ConsumerRequest<T>> extends AllegroRequest<T>
{
  private ConsumerManager consumerManager_ = new ConsumerManager();
  
  /**
   * Constructor.
   * 
   * @param type The concrete type of this class, needed for fluent methods.
   */
  public ConsumerRequest(Class<T> type)
  {
    super(type);
  }

  /**
   * Add the given consumer of the given payload type.
   * 
   * @param <CT>      The type of the payload this consumer accepts.
   * @param type      The type of the payload this consumer accepts.
   * @param consumer  A consumer of the given type.
   * 
   * This method accepts a simple consumer, which is a functional interface and is convenient
   * to use in cases where a close notification is not required.
   * 
   * @return This (fluent method).
   */
  public <CT> T withConsumer(Class<CT> type, ISimpleConsumer<CT> consumer)
  {
    consumerManager_.withConsumer(type, consumer);
    
    return self();
  }

  /**
   * Add the given consumer of the given payload type.
   * 
   * @param <CT>      The type of the payload this consumer accepts.
   * @param type      The type of the payload this consumer accepts.
   * @param consumer  A consumer of the given type.
   * 
   * The <code>close()</code> method of all consumers will be called when the request completes.
   * 
   * @return This (fluent method).
   */
  public <CT> T withConsumer(Class<CT> type, IConsumer<CT> consumer)
  {
    consumerManager_.withConsumer(type, consumer);
    
    return self();
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
  public T withDefaultConsumer(ISimpleConsumer<Object> defaultConsumer)
  {
    consumerManager_.withDefaultConsumer(defaultConsumer);
    
    return self();
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
  public T withDefaultConsumer(IConsumer<Object> defaultConsumer)
  {
    consumerManager_.withDefaultConsumer(defaultConsumer);
    
    return self();
  }

  /* package */ void consume(Object object, ITraceContext traceContext, IFundamentalOpener opener)
  {
    consumerManager_.consume(object, traceContext, opener);
  }

  /**
   * 
   * @return All registered consumers.
   */
  public Collection<IConsumer<?>> getConsumers()
  {
    return consumerManager_.getConsumers();
  }

  /**
   * 
   * Close all registered consumers.
   */
  public void closeConsumers()
  {
    consumerManager_.closeConsumers();
  }
}
