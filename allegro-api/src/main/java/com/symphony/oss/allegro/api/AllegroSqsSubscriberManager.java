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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.sqs.AmazonSQS;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.exception.NotImplementedException;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.fugue.aws.sqs.GatewayAmazonSQSClientBuilder;
import com.symphony.oss.fugue.config.Configuration;
import com.symphony.oss.fugue.naming.Name;
import com.symphony.oss.fugue.pubsub.AbstractPullSubscriberManager;
import com.symphony.oss.fugue.pubsub.ISubscription;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;

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

  private AmazonSQS                  sqsClient_;
  private AWSCredentialsProvider     credentials_;
  private ModelRegistry              modelRegistry_;

  private AllegroSqsSubscriberManager(Builder builder)
  {
    super(builder);
    
    credentials_ = builder.credentials_;
    modelRegistry_ = builder.modelRegistry_;

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
    
    builder.sqsBuilder_.withClientConfiguration(configuration);
      
    sqsClient_ = builder.sqsBuilder_.build();
    
  }
  
  /**
   * Concrete builder.
   * 
   * @author Geremia Longobardo
   *
   */
  public static class Builder extends AbstractPullSubscriberManager.Builder<Builder, IAbstractStoredApplicationObject, AllegroSqsSubscriberManager>
  {
    private String                 region_;
    
    private int                   subscriberThreadPoolSize_ = 1; // TODO: default to number of subscriptions
    private int                   handlerThreadPoolSize_    = 1; // TODO: default to 9*subscriberThreadPoolSize_
    private AWSCredentialsProvider        credentials_;
    private GatewayAmazonSQSClientBuilder sqsBuilder_;
    private ModelRegistry              modelRegistry_;

    private String endPoint_;

    private URL proxyUrl_;
    private String proxyUsername_;
    private String proxyPassword_;

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);    
      
      sqsBuilder_ = GatewayAmazonSQSClientBuilder
          .standard();
    }
    
    @Override
    protected String getConfigPath()
    {
      return "/";
    }

    /**
     * Set the AWS region.
     * 
     * @param region The AWS region in which to operate.
     * 
     * @return this (fluent method)
     */
    public Builder withRegion(String region)
    {
      region_ = region;
      
      sqsBuilder_.withRegion(region_);
      
      return self();
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

    public Builder withSubscriberThreadPoolSize(int subscriberThreadPoolSize)
    {
      subscriberThreadPoolSize_ = subscriberThreadPoolSize;
      
      withConfig(new LocalConfiguration());
      
      return self();
    }
    
    public Builder withCredentials(AWSCredentialsProvider credentials)
    {
      credentials_ = credentials;
      
      sqsBuilder_.withCredentials(credentials);
      
      return self();
    }

    public Builder withHandlerThreadPoolSize(int handlerThreadPoolSize)
    {
      handlerThreadPoolSize_ = handlerThreadPoolSize;
      
      return self();
    }
    
    /**
     * Set the API endpoint.
     * 
     * @param endpoint The AWS gateway endpoint for SQS.
     * 
     * @return this (fluent method)
     */
    public Builder withEndpoint(String endpoint)
    {
      endPoint_ = endpoint;
      
      sqsBuilder_.withEndpoint(endPoint_);
      
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

    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
//      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(region_,      "region");
      faultAccumulator.checkNotNull(credentials_, "credentials");
      faultAccumulator.checkNotNull(modelRegistry_, "modelRegistry");
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
      
      AllegroSqsSubscriber subscriber = new AllegroSqsSubscriber(this, sqsClient_ ,subscriptionName.toString(), getTraceFactory(), subscription.getConsumer(),
          getCounter(), createBusyCounter(subscriptionName), credentials_, modelRegistry_);

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
