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

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.symphony.oss.canon.runtime.ModelRegistry;
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
 * @author Geremia Longobardo
 *
 */
public class AllegroSqsSubscriberManager extends AbstractPullSubscriberManager<IAbstractStoredApplicationObject, AllegroSqsSubscriberManager>
implements IAllegroQueryManager
{
  private static final Logger       log_         = LoggerFactory.getLogger(AllegroSqsSubscriberManager.class);

  private List<AllegroSqsSubscriber> subscribers_ = new LinkedList<>();

  private AllegroSqsFeedsContainer     feeds_;
  private ModelRegistry              modelRegistry_;

  private ObjectHttpModelClient  objectApiClient_;
  private CloseableHttpClient    apiHttpClient_;

  private AllegroSqsSubscriberManager(Builder builder)
  {
    super(builder);
    
    feeds_         = builder.feeds_;
    modelRegistry_ = builder.modelRegistry_;
    objectApiClient_ = builder.objectApiClient_;
    apiHttpClient_ = builder.apiHttpClient_;

    ClientConfiguration configuration = new ClientConfiguration()
    .withMaxConnections(200);

    if(builder.proxyUrl_ !=null) 
    {
      configuration.setProxyHost(builder.proxyUrl_.getHost());
      configuration.setProxyPort(builder.proxyUrl_.getPort());
    }
    
    if(builder.proxyUsername_ != null) 
      configuration.setProxyUsername(builder.proxyUsername_);
    
    if(builder.proxyPassword_ != null)
      configuration.setProxyPassword(builder.proxyPassword_);
    
  }
  
  /**
   * Concrete builder.
   * 
   * @author Geremia Longobardo
   *
   */
  public static class Builder extends AbstractPullSubscriberManager.Builder<Builder, IAbstractStoredApplicationObject, AllegroSqsSubscriberManager>
  {
    
    private int                   subscriberThreadPoolSize_ = 1; // TODO: default to number of subscriptions
    private int                   handlerThreadPoolSize_    = 1; // TODO: default to 9*subscriberThreadPoolSize_
    private AllegroSqsFeedsContainer        feeds_;
    private ModelRegistry              modelRegistry_;

    private URL proxyUrl_;
    private String proxyUsername_;
    private String proxyPassword_;
    
    private ObjectHttpModelClient  objectApiClient_;
    private CloseableHttpClient    apiHttpClient_;

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
    
    /**
     * Set the Model Registry
     * 
     * @param modelRegistry The ModelRegistry set in Allegro
     * 
     * @return this (fluent method)
     */
    public Builder withModelRegistry(ModelRegistry modelRegistry)
    {
      modelRegistry_ = modelRegistry;
      
      return self();
    }

    /**
     * Set the N of threads for subscribers
     * 
     * @param subscriberThreadPoolSize the N of threads
     *  
     * @return this (fluent method)
     */
    public Builder withSubscriberThreadPoolSize(int subscriberThreadPoolSize)
    {
      subscriberThreadPoolSize_ = subscriberThreadPoolSize;
      
      withConfig(new LocalConfiguration());
      
      return self();
    }
    
    
    /**
     * Sets the feeds to be fetched
     * 
     * @param feeds The feed container
     * 
     * @return this (fluent method)
     */
    public Builder withFeedsContainer(AllegroSqsFeedsContainer feeds)
    {
      feeds_ = feeds;
      
      return self();
    }
    
    /**
     * Set the N of threads for handlers
     * 
     * @param handlerThreadPoolSize the N of threads
     *  
     * @return this (fluent method)
     */
    public Builder withHandlerThreadPoolSize(int handlerThreadPoolSize)
    {
      handlerThreadPoolSize_ = handlerThreadPoolSize;
      
      return self();
    }
    
    /**
     * Set the API proxy URL.
     * 
     * @param proxyUrl The client proxy Url.
     * 
     * @return this (fluent method)
     */
    public Builder withProxyUrl(URL proxyUrl)
    {
      proxyUrl_ = proxyUrl; 
      
      return self();
    }

    /**
     * Set the API proxy URL.
     * 
     * @param proxyUsername The client proxy Username.
     * 
     * @return this (fluent method)
     */
    public Builder withProxyUsername(String proxyUsername)
    {
      proxyUsername_ = proxyUsername;  
      
      return self();
    }
    
    /**
     * Set the API proxy password.
     * 
     * @param proxyPassword The client proxy Password.
     * 
     * @return this (fluent method)
     */
    public Builder withProxyPassword(String proxyPassword)
    {
      proxyPassword_ = proxyPassword;   
      
      return self();
    }
    
    /**
     * Set the API client
     * 
     * @param objectApiClient The client needed to connect to API.
     * 
     * @return this (fluent method)
     */
    public Builder withApiClient(ObjectHttpModelClient objectApiClient)
    {
      objectApiClient_ = objectApiClient;
      
      return self();
    }

    /**
     * Set the Http client
     * 
     * @param apiHttpClient The client needed to make http requests
     * 
     * @return this (fluent method)
     */
    public Builder withHttpClient(CloseableHttpClient apiHttpClient)
    {
      apiHttpClient_ = apiHttpClient;
      
      return self();
    }

    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
//      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(feeds_, "credentials");
      faultAccumulator.checkNotNull(modelRegistry_, "modelRegistry");
      faultAccumulator.checkNotNull(objectApiClient_, "objectApiClient");
      faultAccumulator.checkNotNull(apiHttpClient_, "apiHttpClient");
    }
    
    class LocalConfiguration extends Configuration
    {
      protected LocalConfiguration()
      {
        super(new HashMap<String, Object>() 
          {
            {
                put("subscriberThreadPoolSize", new Integer(subscriberThreadPoolSize_));
                put("handlerThreadPoolSize",    new Integer(handlerThreadPoolSize_));
            }
          }
         );
      }
    }

    @Override
    protected AllegroSqsSubscriberManager construct()
    {
      withConfig(new LocalConfiguration());
      return new AllegroSqsSubscriberManager(this);
    }
  }

  @Override
  protected void initSubscription(ISubscription<IAbstractStoredApplicationObject> subscription)
  {
    for(Name subscriptionName : subscription.getSubscriptionNames())
    {
      log_.info("Subscribing to " + subscriptionName + "..."); 
      
      AllegroSqsSubscriber subscriber = new AllegroSqsSubscriber(this, objectApiClient_, apiHttpClient_,subscriptionName.toString(), getTraceFactory(), subscription.getConsumer(),
          getCounter(), createBusyCounter(subscriptionName), feeds_, modelRegistry_);

      subscribers_.add(subscriber); 
    }
  }

  @Override
  protected void startSubscriptions()
  {
    for(AllegroSqsSubscriber subscriber : subscribers_)
    {
      log_.info("Starting subscription to " + subscriber.getQueue() + "...");
      submit(subscriber, true);
    }
  }

  @Override
  protected void stopSubscriptions()
  {
     for(AllegroSqsSubscriber subscriber : subscribers_)
        subscriber.stop();
      
     super.stopSubscriptions();
     
     for(AllegroSqsSubscriber subscriber : subscribers_)
       subscriber.close();
  }

  @Override
  public boolean isIdle()
  {
    throw new NotImplementedException();
  }

  @Override
  public void waitUntilIdle()
  {
    throw new NotImplementedException();
  }
}
