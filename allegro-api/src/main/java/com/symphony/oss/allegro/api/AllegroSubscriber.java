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

import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.Fugue;
import com.symphony.oss.fugue.counter.IBusyCounter;
import com.symphony.oss.fugue.counter.ICounter;
import com.symphony.oss.fugue.pipeline.IThreadSafeRetryableConsumer;
import com.symphony.oss.fugue.pubsub.AbstractPullSubscriber;
import com.symphony.oss.fugue.pubsub.IPullSubscriberContext;
import com.symphony.oss.fugue.pubsub.IPullSubscriberMessage;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.fugue.trace.ITraceContextTransaction;
import com.symphony.oss.fugue.trace.ITraceContextTransactionFactory;
import com.symphony.oss.models.object.canon.FeedRequest;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;
import com.symphony.oss.models.object.canon.facade.FeedObjectDelete;
import com.symphony.oss.models.object.canon.facade.FeedObjectExtend;
import com.symphony.oss.models.object.canon.facade.IFeedObject;

/**
 * An SWS SNS subscriber.
 * 
 * @author Bruce Skingle
 *
 */
class AllegroSubscriber extends AbstractPullSubscriber
{
  private static final int                                                     EXTENSION_TIMEOUT_SECONDS  = 30;
  private static final int                                                     EXTENSION_FREQUENCY_MILLIS = 15000;

  private static final Logger                                                  log_                       = LoggerFactory
      .getLogger(AllegroSubscriber.class);

  private final AllegroSubscriberManager                                       manager_;
  private final ObjectHttpModelClient                                          objectApiClient_;
  private final CloseableHttpClient                                            httpClient_;
  private final Hash                                                           feedHash_;
  private final ITraceContextTransactionFactory                                traceFactory_;
  private final IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> consumer_;
  private final NonIdleSubscriber                                              nonIdleSubscriber_;
  private int                                                                  messageBatchSize_          = 10;


  AllegroSubscriber(AllegroSubscriberManager manager,
      ObjectHttpModelClient systemApiClient,
      CloseableHttpClient httpClient,
      String feedHash,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> consumer, 
      ICounter counter, IBusyCounter busyCounter
      )
  {
    super(manager, feedHash, counter, busyCounter, EXTENSION_FREQUENCY_MILLIS, consumer);
    
    if(Fugue.isDebugSingleThread())
    {
      messageBatchSize_ = 1;
    }
    
    manager_ = manager;
    objectApiClient_ = systemApiClient;
    httpClient_ = httpClient;
    feedHash_ = Hash.newInstance(feedHash);
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
      return pull(0, messageBatchSize_);
    }

    @Override
    public Collection<IPullSubscriberMessage> blockingPull()
    {
      return pull(20, messageBatchSize_);
    }

    private Collection<IPullSubscriberMessage> pull(int waitTimeSeconds, int maxItems)
    {
      List<IPullSubscriberMessage>result = new LinkedList<>();
      
      try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction("FetchFeed", feedHash_.toString()))
      {
        ITraceContext trace = traceTransaction.open();
        
        log_.info("Pull....");
        trace.trace("PULL_MSG");
        List<IFeedObject> messages  = objectApiClient_.newFeedsFeedHashObjectsPostHttpRequestBuilder()
            .withFeedHash(feedHash_)
            .withCanonPayload(new FeedRequest.Builder()
                .withWaitTimeSeconds(waitTimeSeconds)
                .withMaxItems(maxItems)
                .build())
            .build()
            .execute(httpClient_);
        
        trace.trace("RECEIVED_MSG");
        log_.info("Received " + messages.size() + " messages.");
        for(IFeedObject message : messages)
        {
          result.add(new AllegroPullSubscriberMessage(message, trace));
        }
      }
      
      return result;
    }

    @Override
    public void close()
    {
      // Nothing
    }
  }

  public Hash getFeedHash()
  {
    return feedHash_;
  }

  private class AllegroPullSubscriberMessage implements IPullSubscriberMessage
  {
    private final IFeedObject   message_;
    private final ITraceContext trace_;
    private boolean             running_ = true;
    
    private AllegroPullSubscriberMessage(IFeedObject message, ITraceContext trace)
    {
      message_ = message;
      trace_= trace;
    }

    @Override
    public String getMessageId()
    {
      return message_.getMessageId();
    }

    @Override
    public void run()
    {
      try(ITraceContextTransaction traceTransaction = trace_.createSubContext("PubSub:SQS", getMessageId()))
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
              
              objectApiClient_.newFeedsFeedHashObjectsPostHttpRequestBuilder()
                .withFeedHash(feedHash_)
                .withCanonPayload(new FeedRequest.Builder()
                    .withWaitTimeSeconds(0)
                    .withMaxItems(0)
                    .withDelete(new FeedObjectDelete.Builder()
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
              
              objectApiClient_.newFeedsFeedHashObjectsPostHttpRequestBuilder()
              .withFeedHash(feedHash_)
              .withCanonPayload(new FeedRequest.Builder()
                  .withWaitTimeSeconds(0)
                  .withMaxItems(0)
                  .withExtend(new FeedObjectExtend.Builder()
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
          objectApiClient_.newFeedsFeedHashObjectsPostHttpRequestBuilder()
          .withFeedHash(feedHash_)
          .withCanonPayload(new FeedRequest.Builder()
              .withWaitTimeSeconds(0)
              .withMaxItems(0)
              .withExtend(new FeedObjectExtend.Builder()
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
