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

import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.canon.runtime.ModelRegistry;
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
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;

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

  protected final ModelRegistry                                                modelRegistry_;         
  private final AllegroSqsSubscriberManager                                    manager_;
  private final AmazonSQS                                                      sqsClient_;
  private final String                                                         queueUrl_;
  private final ITraceContextTransactionFactory                                traceFactory_;
  private final IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> consumer_;
  private final NonIdleSubscriber                                              nonIdleSubscriber_;
  private int                                                                  messageBatchSize_          = 10;
  private ReceiveMessageRequest                                                blockingPullRequest_;
  private ReceiveMessageRequest                                                nonBlockingPullRequest_;
  private AWSCredentialsProvider                                               credentials_;


  AllegroSqsSubscriber(AllegroSqsSubscriberManager manager,
      AmazonSQS sqsClient, String queueUrl,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> consumer, 
      ICounter counter, IBusyCounter busyCounter,
      AWSCredentialsProvider credentials, ModelRegistry modelRegistry )
  {
    super(manager, queueUrl, counter, busyCounter, EXTENSION_FREQUENCY_MILLIS, consumer);
    
    if(Fugue.isDebugSingleThread())
    {
      messageBatchSize_ = 1;
    }
    
    sqsClient_         = sqsClient;
    manager_           = manager;
    queueUrl_          =  queueUrl;
    traceFactory_      = traceFactory;
    consumer_          = consumer;
    nonIdleSubscriber_ = new NonIdleSubscriber();
    credentials_       = credentials;
    
    blockingPullRequest_ = new ReceiveMessageRequest(queueUrl)
        .withMaxNumberOfMessages(messageBatchSize_ )
        .withWaitTimeSeconds(20);
    
    nonBlockingPullRequest_ = new ReceiveMessageRequest(queueUrl)
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
    return queueUrl_;
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
          credentials_.refresh();
          ReceiveMessageResult receiveResult = sqsClient_.receiveMessage(pullRequest);
          
          trace.trace("RECEIVED_SQS");
          for(Message receivedMessage : receiveResult.getMessages())
          {
            result.add(new AllegroPullSubscriberMessage(receivedMessage, trace));
          }
        
        } catch(QueueDoesNotExistException e)
        {
          trace.trace("Stopping Subscriber, Queue deleted: "+queueUrl_);
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
    private final Message message_;
    private boolean       running_ = true;
    private ITraceContext trace_;
   
    private AllegroPullSubscriberMessage(Message message, ITraceContext trace)
    {
      message_ = message;
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
        IEntity entity = modelRegistry_.parseOne(new StringReader(message_.getBody()));
        
        if(entity instanceof IAbstractStoredApplicationObject)
        {

          IAbstractStoredApplicationObject object = (IAbstractStoredApplicationObject) entity;
          long retryTime = manager_.handleMessage(consumer_, object, trace, message_.getMessageId());
          
          credentials_.refresh();
          
          synchronized(this)
          {
            // There is no point trying to extend the ack deadline now
            running_ = false;
  
            
            if(retryTime < 0)
            {
              trace.trace("ABOUT_TO_ACK");
              sqsClient_.deleteMessage(queueUrl_, message_.getReceiptHandle());
              traceTransaction.finished();
            }
            else
            {
              trace.trace("ABOUT_TO_NACK");
              
              int visibilityTimout = (int) (retryTime / 1000);
              
              sqsClient_.changeMessageVisibility(queueUrl_, message_.getReceiptHandle(), visibilityTimout);
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
          credentials_.refresh();
          
          sqsClient_.changeMessageVisibility(queueUrl_, message_.getReceiptHandle(), EXTENSION_TIMEOUT_SECONDS);
        }
        catch(RuntimeException e)
        {
          log_.error("Failed to extend message " + getMessageId(), e);
        }
      }
    }
  }
}
