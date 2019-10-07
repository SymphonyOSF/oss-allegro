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

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeSimpleErrorConsumer;

import com.symphony.oss.models.fundamental.canon.facade.INotification;

/**
 * A request object for the CreateFeedSubscriber method.
 * 
 * For a low level implementation of this feature see FetchFeedMessagesRequest
 * 
 * @author Bruce Skingle
 *
 */
public class CreateFeedSubscriberRequest  extends ThreadSafeConsumerRequest<CreateFeedSubscriberRequest>
{
  private String                                  name_;
  private IThreadSafeErrorConsumer<INotification> unprocessableMessageConsumer_;
  private int                                     subscriberThreadPoolSize_ = 1;
  private int                                     handlerThreadPoolSize_    = 1;
  
  /**
   * Constructor.
   */
  public CreateFeedSubscriberRequest()
  {
    super(CreateFeedSubscriberRequest.class);
  }

  /**
   * 
   * @return The name of the feed to be read.
   */
  public String getName()
  {
    return name_;
  }

  /**
   * Set the name of the feed to be read.
   * 
   * @param name The name of the feed to be read.
   * 
   * @return This (fluent method)
   */
  public CreateFeedSubscriberRequest withName(String name)
  {
    name_ = name;
    
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
  public CreateFeedSubscriberRequest withSubscriberThreadPoolSize(int subscriberThreadPoolSize)
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
  public CreateFeedSubscriberRequest withHandlerThreadPoolSize(int handlerThreadPoolSize)
  {
    handlerThreadPoolSize_ = handlerThreadPoolSize;
    
    return self();
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
   * Set the consumer to which unprocessable messages will be directed.
   * 
   * @param unprocessableMessageConsumer The consumer to which unprocessable messages will be directed.
   * 
   * @return This (fluent method)
   */
  public CreateFeedSubscriberRequest withUnprocessableMessageConsumer(IThreadSafeErrorConsumer<INotification> unprocessableMessageConsumer)
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
  public CreateFeedSubscriberRequest withUnprocessableMessageConsumer(IThreadSafeSimpleErrorConsumer<INotification> unprocessableMessageConsumer)
  {
    unprocessableMessageConsumer_ = new IThreadSafeErrorConsumer<INotification>()
        {

          @Override
          public void consume(INotification item, ITraceContext trace, String message, Throwable cause)
          {
            unprocessableMessageConsumer.consume(item, trace, message, cause);
          }

          @Override
          public void close(){}
        };
    
    return self();
  }

  /**
   * 
   * @return The consumer to which unprocessable messages will be directed.
   */
  public IThreadSafeErrorConsumer<INotification> getUnprocessableMessageConsumer()
  {
    return unprocessableMessageConsumer_;
  }

  @Override
  public void validate()
  {
    super.validate();
    
    require(name_, "Name");
  }
}
