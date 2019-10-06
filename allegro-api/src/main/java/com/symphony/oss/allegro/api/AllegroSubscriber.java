/*
 *
 *
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.symphony.oss.allegro.api;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.canon.runtime.exception.NotFoundException;
import org.symphonyoss.s2.fugue.Fugue;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.IBusyCounter;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pubsub.AbstractPullSubscriber;
import org.symphonyoss.s2.fugue.pubsub.IPullSubscriberContext;
import org.symphonyoss.s2.fugue.pubsub.IPullSubscriberMessage;

import com.symphony.oss.models.fundamental.canon.facade.INotification;
import com.symphony.oss.models.system.canon.FeedRequest;
import com.symphony.oss.models.system.canon.IFeedRequest;
import com.symphony.oss.models.system.canon.SystemHttpModelClient;
import com.symphony.oss.models.system.canon.facade.FeedMessageDelete;
import com.symphony.oss.models.system.canon.facade.FeedMessageExtend;
import com.symphony.oss.models.system.canon.facade.IFeedMessage;

/**
 * An SWS SNS subscriber.
 * 
 * @author Bruce Skingle
 *
 */
/* package */ class AllegroSubscriber extends AbstractPullSubscriber
{
  private static final int EXTENSION_TIMEOUT_SECONDS = 30;
  private static final int EXTENSION_FREQUENCY_MILLIS = 15000;
  
  private static final Logger                        log_              = LoggerFactory.getLogger(AllegroSubscriber.class);

  private final AllegroSubscriberManager                    manager_;
  private final SystemHttpModelClient                       systemApiClient_;
  private final CloseableHttpClient                         httpClient_;
  private final String                                      feedName_;
  private final ITraceContextTransactionFactory             traceFactory_;
  private final IThreadSafeRetryableConsumer<INotification> consumer_;
  private final NonIdleSubscriber                           nonIdleSubscriber_;
  private int                                               messageBatchSize_          = 10;


  /* package */ AllegroSubscriber(AllegroSubscriberManager manager,
      SystemHttpModelClient systemApiClient,
      CloseableHttpClient httpClient,
      String feedName,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeRetryableConsumer<INotification> consumer, 
      ICounter counter, IBusyCounter busyCounter
      )
  {
    super(manager, feedName, counter, busyCounter, EXTENSION_FREQUENCY_MILLIS, consumer);
    
    if(Fugue.isDebugSingleThread())
    {
      messageBatchSize_ = 1;
    }
    
    manager_ = manager;
    systemApiClient_ = systemApiClient;
    httpClient_ = httpClient;
    feedName_ = feedName;
    traceFactory_ = traceFactory;
    consumer_ = consumer;
    nonIdleSubscriber_ = new NonIdleSubscriber();
  }
  
  class NonIdleSubscriber implements Runnable
  {
    @Override
    public void run()
    {
      AllegroSubscriber.this.run(false);
    }
  }

  @Override
  protected NonIdleSubscriber getNonIdleSubscriber()
  {
    return nonIdleSubscriber_;
  }
  
  @Override
  protected IPullSubscriberContext getContext()
  {
    return new AllegroPullSubscriberContext();
  }

  private class AllegroPullSubscriberContext implements IPullSubscriberContext
  {
    @Override
    public Collection<IPullSubscriberMessage> nonBlockingPull()
    {
      return pull(new FeedRequest.Builder()
          .withWaitTimeSeconds(0)
          .withMaxMessages(messageBatchSize_)
          .build());
    }

    @Override
    public Collection<IPullSubscriberMessage> blockingPull()
    {
      return pull(new FeedRequest.Builder()
          .withWaitTimeSeconds(20)
          .withMaxMessages(messageBatchSize_)
          .build());
    }

    private Collection<IPullSubscriberMessage> pull(IFeedRequest request)
    {
      List<IPullSubscriberMessage>result = new LinkedList<>();
      
      try
      {
        for(IFeedMessage receivedMessage :  systemApiClient_.newFeedsNameMessagesPostHttpRequestBuilder()
            .withName(feedName_)
            .withCanonPayload(request)
            .build()
            .execute(httpClient_))
        {
          result.add(new AllegroPullSubscriberMessage(receivedMessage));
        }
      }
      catch(NotFoundException e)
      {
      }
      
      return result;
    }

    @Override
    public void close()
    {
      // Nothing
    }
  }

  public String getFeedName()
  {
    return feedName_;
  }

  private class AllegroPullSubscriberMessage implements IPullSubscriberMessage
  {
    private final IFeedMessage message_;
    private boolean       running_ = true;
    
    private AllegroPullSubscriberMessage(IFeedMessage message)
    {
      message_ = message;
    }

    @Override
    public String getMessageId()
    {
      return message_.getMessageId();
    }

    @Override
    public void run()
    {
      try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction("PubSub:SQS", getMessageId(), null))
      {
        ITraceContext trace = traceTransaction.open();
        
        long retryTime = manager_.handleMessage(consumer_, message_.getPayload(), trace, getMessageId());
        
        synchronized(this)
        {
          // There is no point trying to extend the ack deadline now
          running_ = false;

          if(retryTime < 0)
          {
            try
            {
              trace.trace("ABOUT_TO_ACK");
              
              systemApiClient_.newFeedsNameMessagesPostHttpRequestBuilder()
                .withName(feedName_)
                .withCanonPayload(new FeedRequest.Builder()
                    .withWaitTimeSeconds(0)
                    .withMaxMessages(0)
                    .withDelete(new FeedMessageDelete.Builder()
                        .withReceiptHandle(message_.getReceiptHandle())
                        .build())
                    .build())
                .build()
                .execute(httpClient_);
            }
            catch(NotFoundException e)
            {
              // expected
            }
            
            traceTransaction.finished();
          }
          else
          {
            try
            {
              trace.trace("ABOUT_TO_NACK");
            
              int visibilityTimout = (int) (retryTime / 1000);
              
              systemApiClient_.newFeedsNameMessagesPostHttpRequestBuilder()
              .withName(feedName_)
              .withCanonPayload(new FeedRequest.Builder()
                  .withWaitTimeSeconds(0)
                  .withMaxMessages(0)
                  .withExtend(new FeedMessageExtend.Builder()
                      .withReceiptHandle(message_.getReceiptHandle())
                      .withVisibilityTimeout(visibilityTimout)
                      .build())
                  .build())
              .build()
              .execute(httpClient_);
            }
            catch(NotFoundException e)
            {
              // expected
            }
            
            traceTransaction.aborted();
          }
        }
      }
      catch(RuntimeException e)
      {
        log_.error("Failed to process message " + getMessageId(), e);
      }
    }

    @Override
    public synchronized void extend()
    {
      if(running_)
      {
        try
        {
          systemApiClient_.newFeedsNameMessagesPostHttpRequestBuilder()
          .withName(feedName_)
          .withCanonPayload(new FeedRequest.Builder()
              .withWaitTimeSeconds(0)
              .withMaxMessages(0)
              .withExtend(new FeedMessageExtend.Builder()
                  .withReceiptHandle(message_.getReceiptHandle())
                  .withVisibilityTimeout(EXTENSION_TIMEOUT_SECONDS)
                  .build())
              .build())
          .build()
          .execute(httpClient_);
          log_.info("Extended message " + getMessageId());
        }
        catch(NotFoundException e)
        {
          log_.info("Extended message " + getMessageId());
        }
        catch(RuntimeException e)
        {
          log_.error("Failed to extend message " + getMessageId(), e);
        }
      }
    }
  }
}
