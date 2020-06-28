/*
 *
 *
 * Copyright 2020 Symphony Communication Services, LLC.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.http.client.IAuthenticationProvider;
import com.symphony.oss.canon.runtime.http.client.IResponseHandler;
import com.symphony.oss.canon.runtime.jjwt.JwtBase;
import com.symphony.oss.fugue.pipeline.FatalConsumerException;
import com.symphony.oss.fugue.pipeline.RetryableConsumerException;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.models.internal.pod.canon.AckId;
import com.symphony.oss.models.internal.pod.canon.AckIdObject;
import com.symphony.oss.models.internal.pod.canon.FeedId;
import com.symphony.oss.models.internal.pod.canon.IAckIdObject;
import com.symphony.oss.models.internal.pod.canon.IEvent;
import com.symphony.oss.models.internal.pod.canon.IEvents;
import com.symphony.oss.models.internal.pod.canon.IFeed;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;

class AllegroDatafeedClient
{
  private final ServiceTokenManager        serviceTokenManager_;
  private final ModelRegistry              modelRegistry_;
  private final CloseableHttpClient        httpClient_;
  private final PodInternalHttpModelClient datafeed2ApiClient_;

  private String                           datafeed2Token_;
  
  AllegroDatafeedClient(ServiceTokenManager serviceTokenManager, ModelRegistry modelRegistry, CloseableHttpClient httpClient, URL podUrl, Map<Integer, IResponseHandler> responseHandlerMap)
  {
    serviceTokenManager_ = serviceTokenManager;
    modelRegistry_ = modelRegistry;
    httpClient_ = httpClient;
    
    IAuthenticationProvider datafeed2JwtGenerator  = new IAuthenticationProvider()
    {
      @Override
      public void authenticate(RequestBuilder builder)
      {
        builder.addHeader(JwtBase.AUTH_HEADER_KEY, JwtBase.AUTH_HEADER_VALUE_PREFIX + datafeed2Token_);
      }
    };
    
    datafeed2ApiClient_ = new PodInternalHttpModelClient(
        modelRegistry_,
        podUrl, null, datafeed2JwtGenerator, responseHandlerMap);
  }
  
  List<FeedId> listFeeds()
  {
    refreshTokenIfNecessary();
    
    List<IFeed> feeds = datafeed2ApiClient_.newDatafeed2ApiV1FeedsGetHttpRequestBuilder()
      .build()
      .execute(httpClient_)
      ;
    
    List<FeedId>  feedIds = new ArrayList<>(feeds.size());
    
    for(IFeed feed : feeds)
    {
      feedIds.add(feed.getFeedId());
    }
    
    return feedIds;
  }
  
  private void refreshTokenIfNecessary()
  {
    datafeed2Token_ = serviceTokenManager_.getServiceToken("datafeed2");
  }

  FeedId createFeed()
  { 
    refreshTokenIfNecessary();
    
    IFeed feed = datafeed2ApiClient_.newDatafeed2ApiV1FeedsPostHttpRequestBuilder()
        .build()
        .execute(httpClient_)
        ;
      
    return feed.getFeedId();
  }
  
  AckId fetchFeedEvents(FeedId feedId, @Nullable AckId ackId, AbstractConsumerManager consumerManager, AllegroDecryptor opener, ITraceContext trace)
  {
    refreshTokenIfNecessary();
    
    IAckIdObject canonPayload = new AckIdObject.Builder()
        .withAckId(ackId)
        .build();
    
    IEvents events = datafeed2ApiClient_.newDatafeed2ApiV1FeedsFeedIdEventsPostHttpRequestBuilder()
      .withFeedId(feedId)
      .withCanonPayload(canonPayload)
      .build()
      .execute(httpClient_);
    
    for(IEvent event : events.getEvents())
    {
      try
      {
        consumerManager.consume(event.getPayload(), trace, opener);
      }
      catch (RetryableConsumerException | FatalConsumerException e)
      {
        consumerManager.getUnprocessableMessageConsumer().consume(event.getPayload(), trace, "Failed to process message", e);
      }
    }
    
    return events.getAckId();
  }
}
