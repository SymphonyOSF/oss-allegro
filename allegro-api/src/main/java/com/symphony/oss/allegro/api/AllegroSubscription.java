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

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.naming.Name;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;
import org.symphonyoss.s2.fugue.pubsub.ISubscription;

import com.google.common.collect.ImmutableSet;
//import com.symphony.oss.models.fundamental.canon.facade.INotification;

/* package */ class AllegroSubscription //implements ISubscription<INotification>
{
//  private final IThreadSafeRetryableConsumer<INotification> consumer_;
//  private final ImmutableSet<FeedName>                      subscriptionNames_;
//
//  public AllegroSubscription(CreateFeedSubscriberRequest request, AllegroApi allegroApi)
//  {
//    subscriptionNames_ = ImmutableSet.of(new FeedName(request.getName()));
//    
//    consumer_ = new IThreadSafeRetryableConsumer<INotification>()
//    {
//      @Override
//      public void consume(INotification item, ITraceContext trace)
//          throws RetryableConsumerException, FatalConsumerException
//      {
//        request.consume(item, trace, allegroApi);
//      }
//
//      @Override
//      public void close()
//      {
//        request.closeConsumers();
//      }
//    };
//  }
//  
//  class FeedName extends Name
//  {
//    protected FeedName(String name)
//    {
//      super(name);
//    }
//  }
//
//  @Override
//  public ImmutableSet<? extends Name> getSubscriptionNames()
//  {
//    return subscriptionNames_;
//  }
//
//  @Override
//  public IThreadSafeRetryableConsumer<INotification> getConsumer()
//  {
//    return consumer_;
//  }

}
