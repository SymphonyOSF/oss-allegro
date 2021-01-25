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

package com.symphony.oss.allegro.objectstore;

import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.fugue.Fugue;
import com.symphony.oss.fugue.aws.sqs.SqsAction;
import com.symphony.oss.fugue.aws.sqs.SqsResponseMessage;
import com.symphony.oss.fugue.counter.IBusyCounter;
import com.symphony.oss.fugue.counter.ICounter;
import com.symphony.oss.fugue.pipeline.IThreadSafeRetryableConsumer;
import com.symphony.oss.fugue.pubsub.AbstractPullSubscriber;
import com.symphony.oss.fugue.pubsub.IPullSubscriberContext;
import com.symphony.oss.fugue.pubsub.IPullSubscriberMessage;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.fugue.trace.ITraceContextTransaction;
import com.symphony.oss.fugue.trace.ITraceContextTransactionFactory;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;

/**
 * An SWS SNS subscriber.
 * 
 * @author Geremia Longobardo
 *
 */
class AllegroSqsSubscriber extends AbstractPullSubscriber
{
  private static final int                                                     EXTENSION_TIMEOUT_SECONDS  = 30;
  private static final int                                                     EXTENSION_FREQUENCY_MILLIS = 15000;

  private static final Logger                                                  log_                       = LoggerFactory
      .getLogger(AllegroSqsSubscriber.class);

  private final ModelRegistry                                                  modelRegistry_;         
  private final AllegroSqsSubscriberManager                                    manager_;
  private final String                                                         feedHash_;
  private final ITraceContextTransactionFactory                                traceFactory_;
  private final IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> consumer_;
  private final NonIdleSubscriber                                              nonIdleSubscriber_;
  private int                                                                  messageBatchSize_          = 10;
  private ReceiveMessageRequest                                                blockingPullRequest_;
  private ReceiveMessageRequest                                                nonBlockingPullRequest_;
  private AllegroSqsFeedsContainer                                             feeds_;
  private ObjectHttpModelClient                                                objectApiClient_;
  private CloseableHttpClient                                                  apiHttpClient_;
  private IBaseObjectStoreApi                                                  allegro_;

  AllegroSqsSubscriber(AllegroSqsSubscriberManager manager,
      ObjectHttpModelClient objectApiClient,  CloseableHttpClient apiHttpClient, String feedHash,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> consumer, 
      ICounter counter, IBusyCounter busyCounter,
      AllegroSqsFeedsContainer feeds, ModelRegistry modelRegistry )
  {
    super(manager, feedHash, counter, busyCounter, EXTENSION_FREQUENCY_MILLIS, consumer);
    
    if(Fugue.isDebugSingleThread())
    {
      messageBatchSize_ = 1;
    }
    objectApiClient_   = objectApiClient;
    apiHttpClient_     = apiHttpClient;
    manager_           = manager;
    feedHash_          =  feedHash;
    traceFactory_      = traceFactory;
    consumer_          = consumer;
    nonIdleSubscriber_ = new NonIdleSubscriber();
    feeds_             = feeds;
    allegro_           = feeds.getAllegro();
    
    blockingPullRequest_ = new ReceiveMessageRequest(feedHash)
        .withMaxNumberOfMessages(messageBatchSize_ )
        .withWaitTimeSeconds(20);
    
    nonBlockingPullRequest_ = new ReceiveMessageRequest(feedHash)
        .withMaxNumberOfMessages(messageBatchSize_ );
    
    modelRegistry_  = modelRegistry;
  }

  class NonIdleSubscriber implements Runnable
  {
    @Override
    public void run()
    {
      AllegroSqsSubscriber.this.run(false);
    }
  }
  
  public String getQueue() {
    return feedHash_;
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
      return pull(nonBlockingPullRequest_);
    }

    @Override
    public Collection<IPullSubscriberMessage> blockingPull()
    {
      return pull(blockingPullRequest_);
    }

    private Collection<IPullSubscriberMessage> pull(ReceiveMessageRequest pullRequest)
    {
      
      try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction("PubSubPull:SQS", UUID.randomUUID().toString(), null))
      {
        ITraceContext trace = traceTransaction.open();
        
        List<IPullSubscriberMessage> result = new LinkedList<>();
        try 
        { 
          feeds_.refresh();
          
          List<SqsResponseMessage> messages = new AllegroSqsRequestBuilder(allegro_, feeds_.getEndpoint())
              .withFeedHash(feedHash_)
              .withAction(SqsAction.RECEIVE)
              .withMaxNumberOfMessages(pullRequest.getMaxNumberOfMessages())
              .withWaitTimeSeconds(pullRequest.getWaitTimeSeconds())          
            .execute(apiHttpClient_);

          trace.trace("RECEIVED_SQS");

          for(SqsResponseMessage receivedMessage : messages)
          {
            result.add(new AllegroPullSubscriberMessage(receivedMessage, trace));
          }
        
        } catch(QueueDoesNotExistException e)
        {
          trace.trace("Stopping Subscriber, Feed deleted: "+feedHash_);
          running_ = false;
        }
        
        return result;
      }

    }

    @Override
    public void close()
    {
      // Nothing
    }
  }


  private class AllegroPullSubscriberMessage implements IPullSubscriberMessage
  {
    private final SqsResponseMessage message_;
    private boolean       running_ = true;
    private ITraceContext trace_;
   
    private AllegroPullSubscriberMessage(SqsResponseMessage receivedMessage, ITraceContext trace)
    {
      message_ = receivedMessage;
      trace_ = trace;
    }

    @Override
    public String getMessageId()
    {
      return message_.getMessageId();
    }

    @Override
    public void run()
    {
      try(ITraceContextTransaction traceTransaction = trace_.createSubContext("PubSubHandle:SQS", message_.getMessageId(), ""))
      {
        ITraceContext trace = traceTransaction.open();
        IEntity entity = modelRegistry_.parseOne(new StringReader(message_.getPayload()));
        
        if(entity instanceof IAbstractStoredApplicationObject)
        {

          IAbstractStoredApplicationObject object = (IAbstractStoredApplicationObject) entity;
          long retryTime = manager_.handleMessage(consumer_, object, trace, message_.getMessageId());
          
          feeds_.refresh();
          
          synchronized(this)
          {
            // There is no point trying to extend the ack deadline now
            running_ = false;
  
            
            if(retryTime < 0)
            {
              trace.trace("ABOUT_TO_ACK");
              
              new AllegroSqsRequestBuilder(allegro_, feeds_.getEndpoint())
                .withFeedHash(feedHash_)
                .withAction(SqsAction.DELETE)
                .withReceiptHandle(message_.getReceiptHandle())
              .execute(apiHttpClient_);
              
              traceTransaction.finished();
            }
            else
            {
              trace.trace("ABOUT_TO_NACK");
              
              int visibilityTimout = (int) (retryTime / 1000);
              
              new AllegroSqsRequestBuilder(allegro_, feeds_.getEndpoint())
                .withFeedHash(feedHash_)
                .withAction(SqsAction.EXTEND)
                .withReceiptHandle(message_.getReceiptHandle())
                .withVisibilityTimeout(visibilityTimout)
              .execute(apiHttpClient_);
                
              traceTransaction.aborted();
            }
          }
        } else
        {
          log_.error("Retrieved unexpected feed entity of type " + entity.getCanonType());
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
          feeds_.refresh();
          
          new AllegroSqsRequestBuilder(allegro_, feeds_.getEndpoint())
            .withFeedHash(feedHash_)
            .withAction(SqsAction.EXTEND)
            .withReceiptHandle(message_.getReceiptHandle())
            .withVisibilityTimeout(EXTENSION_TIMEOUT_SECONDS)
          .execute(apiHttpClient_);
          
        }
        catch(RuntimeException e)
        {
          log_.error("Failed to extend message " + getMessageId(), e);
        }
      }
    }
  }
}
