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

import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.naming.Name;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;
import org.symphonyoss.s2.fugue.pubsub.ISubscription;

import com.google.common.collect.ImmutableSet;
import com.symphony.oss.allegro.api.request.SubscribeFeedObjectsRequest;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;

/* package */ class AllegroSubscription implements ISubscription<IAbstractStoredApplicationObject>
{
  private final IThreadSafeRetryableConsumer<IAbstractStoredApplicationObject> consumer_;
  private final ImmutableSet<FeedName>                                         subscriptionNames_;

  public AllegroSubscription(SubscribeFeedObjectsRequest request, AllegroApi allegroApi)
  {
    subscriptionNames_ = ImmutableSet.of(new FeedName(request.getHash(allegroApi.getUserId())));
    
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
