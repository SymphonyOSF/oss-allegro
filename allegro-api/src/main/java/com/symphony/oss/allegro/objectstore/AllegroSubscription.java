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

package com.symphony.oss.allegro.objectstore;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.symphony.oss.allegro.api.request.FeedQuery;
import com.symphony.oss.allegro.api.request.FetchFeedObjectsRequest;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.naming.Name;
import com.symphony.oss.fugue.pipeline.FatalConsumerException;
import com.symphony.oss.fugue.pipeline.IThreadSafeRetryableConsumer;
import com.symphony.oss.fugue.pipeline.RetryableConsumerException;
import com.symphony.oss.fugue.pubsub.ISubscription;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;

/* package */ class AllegroSubscription implements ISubscription<IAbstractStoredApplicationObject>
{
  private final IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> consumer_;
  private final ImmutableSet<FeedName>                                         subscriptionNames_;

  public AllegroSubscription(FetchFeedObjectsRequest request, AllegroBaseApi allegroApi)
  {
    Set<FeedName> names = new HashSet<>();
    
    for(FeedQuery query : request.getQueryList())
      names.add(new FeedName(query.getHash(allegroApi.getUserId())));
    
    subscriptionNames_ = ImmutableSet.copyOf(names);
    
    consumer_ = new IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject>()
    {
      @Override
      public void consume(IAbstractStoredApplicationObject item, ITraceContext trace)
          throws RetryableConsumerException, FatalConsumerException
      {
        request.getConsumerManager().consume(item, trace, allegroApi);
      }

      @Override
      public void close()
      {
        request.getConsumerManager().closeConsumers();
      }
    };
  }
  
  class FeedName extends Name
  {
    protected FeedName(Hash hash)
    {
      super(hash.toStringBase64());
    }
  }

  @Override
  public ImmutableSet<? extends Name> getSubscriptionNames()
  {
    return subscriptionNames_;
  }

  @Override
  public IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> getConsumer()
  {
    return consumer_;
  }

}
