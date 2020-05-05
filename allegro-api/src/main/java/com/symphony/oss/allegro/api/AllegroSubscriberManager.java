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

import java.util.LinkedList;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.symphony.oss.canon.runtime.exception.NotImplementedException;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.fugue.config.Configuration;
import com.symphony.oss.fugue.naming.Name;
import com.symphony.oss.fugue.pubsub.AbstractPullSubscriberManager;
import com.symphony.oss.fugue.pubsub.ISubscription;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;

/**
 * Allegro implementation of SubscriberManager.
 * 
 * @author Bruce Skingle
 *
 */
/* package */ class AllegroSubscriberManager extends AbstractPullSubscriberManager<IAbstractStoredApplicationObject, AllegroSubscriberManager>
implements IAllegroQueryManager
{
  private static final Logger       log_         = LoggerFactory.getLogger(AllegroSubscriberManager.class);

  private List<AllegroSubscriber>     subscribers_ = new LinkedList<>();
  private final ObjectHttpModelClient objectApiClient_;
  private final CloseableHttpClient   httpClient_;

  private AllegroSubscriberManager(Builder builder)
  {
    super(builder);
    
    objectApiClient_ = builder.objectApiClient_;
    httpClient_ = builder.httpClient_;
  }
  
  /**
   * Concrete builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractPullSubscriberManager.Builder<Builder, IAbstractStoredApplicationObject, AllegroSubscriberManager>
  {
    private ObjectHttpModelClient objectApiClient_;
    private CloseableHttpClient   httpClient_;
    private int                   subscriberThreadPoolSize_ = 1; // TODO: default to number of subscriptions
    private int                   handlerThreadPoolSize_    = 1; // TODO: default to 9*subscriberThreadPoolSize_

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }
    
    @Override
    protected String getConfigPath()
    {
      return "/";
    }

    public Builder withObjectApiClient(ObjectHttpModelClient objectApiClient)
    {
      objectApiClient_ = objectApiClient;
      
      return self();
    }

    public Builder withHttpClient(CloseableHttpClient httpClient)
    {
      httpClient_ = httpClient;
      
      return self();
    }

    public Builder withSubscriberThreadPoolSize(int subscriberThreadPoolSize)
    {
      subscriberThreadPoolSize_ = subscriberThreadPoolSize;
      
      return self();
    }

    public Builder withHandlerThreadPoolSize(int handlerThreadPoolSize)
    {
      handlerThreadPoolSize_ = handlerThreadPoolSize;
      
      return self();
    }

    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      faultAccumulator.checkNotNull(objectApiClient_, "systemApiClient");
      faultAccumulator.checkNotNull(httpClient_, "httpClient");
      
      withConfig(new LocalConfiguration());
      
      super.validate(faultAccumulator);
    }
    
    class LocalConfiguration extends Configuration
    {
      protected LocalConfiguration()
      {
        super(new ObjectMapper().createObjectNode().put("subscriberThreadPoolSize", subscriberThreadPoolSize_).put("handlerThreadPoolSize", handlerThreadPoolSize_));
      }
    }

    @Override
    protected AllegroSubscriberManager construct()
    {
      return new AllegroSubscriberManager(this);
    }
  }

  @Override
  protected void initSubscription(ISubscription<IAbstractStoredApplicationObject> subscription)
  {
    for(Name subscriptionName : subscription.getSubscriptionNames())
    {
      log_.info("Subscribing to " + subscriptionName + "..."); 
      
      AllegroSubscriber subscriber = new AllegroSubscriber(this, objectApiClient_, httpClient_, subscriptionName.toString(), getTraceFactory(), subscription.getConsumer(),
          getCounter(), createBusyCounter(subscriptionName));

      subscribers_.add(subscriber); 
    }
  }

  @Override
  protected void startSubscriptions()
  {
    for(AllegroSubscriber subscriber : subscribers_)
    {
      log_.info("Starting subscription to " + subscriber.getFeedHash() + "...");
      submit(subscriber, true);
    }
  }

  @Override
  protected void stopSubscriptions()
  {
     for(AllegroSubscriber subscriber : subscribers_)
        subscriber.stop();
      
     super.stopSubscriptions();
     
     for(AllegroSubscriber subscriber : subscribers_)
       subscriber.close();
  }

  @Override
  public boolean isIdle()
  {
    throw new NotImplementedException();
  }

  @Override
  public void waitUntilIdle() throws InterruptedException
  {
    throw new NotImplementedException();
  }
}
