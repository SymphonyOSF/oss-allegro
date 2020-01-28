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

import java.util.List;

import javax.annotation.Nullable;

import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.NoOpTraceContext;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

import com.symphony.oss.allegro.api.request.AbstractConsumerManager;
import com.symphony.oss.allegro.api.request.PartitionQuery;
import com.symphony.oss.models.core.canon.ICursors;
import com.symphony.oss.models.core.canon.IPagination;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

class PartitionObjectPage implements IObjectPage
{
  private final AllegroApi                     allegroApi_;
  private final Hash                           partitionHash_;
  private final PartitionQuery                 query_;
  private final String                         after_;
  private final String                         before_;
  private final List<IStoredApplicationObject> data_;

  public PartitionObjectPage(AllegroApi allegroApi, Hash partitionHash, PartitionQuery query, IPageOfStoredApplicationObject page)
  {
    IPagination pagination = page.getPagination();

    String after  = null;
    String before = null;
    
    if (pagination != null)
    {
      ICursors cursors = pagination.getCursors();

      if (cursors != null)
      {
        if(query.getScanForwards())
        {
          after = cursors.getAfter();
          before = cursors.getBefore();
        }
        else
        {
           before = cursors.getAfter();
           after = cursors.getBefore();
        }
      }
    }
    
    allegroApi_     = allegroApi;
    partitionHash_  = partitionHash;
    query_          = query;
    after_          = after;
    before_         = before;
    data_           = page.getData(); // Actually an ImmutableList.
  }
  
  @Override
  public @Nullable PartitionObjectPage  fetchNextPage()
  {
    if(after_ == null)
      return null;
    
    return allegroApi_.fetchPartitionObjectPage(new PartitionQuery.Builder()
        .withAfter(after_)
        .withHash(partitionHash_)
        .withMaxItems(query_.getMaxItems())
        .withScanForwards(true)
        .withSortKeyPrefix(query_.getSortKeyPrefix())
        .build()
        );
  }
  
  @Override
  public @Nullable PartitionObjectPage  fetchPrevPage()
  {
    if(before_ == null)
      return null;
    
    return allegroApi_.fetchPartitionObjectPage(new PartitionQuery.Builder()
        .withAfter(before_)
        .withHash(partitionHash_)
        .withMaxItems(query_.getMaxItems())
        .withScanForwards(false)
        .withSortKeyPrefix(query_.getSortKeyPrefix())
        .build()
        );
  }
  
  @Override
  public void consume(AbstractConsumerManager consumerManager)
  {
    consume(consumerManager, NoOpTraceContext.INSTANCE);
  }
  
  @Override
  public void consume(AbstractConsumerManager consumerManager, ITraceContext trace)
  {
    for (IAbstractStoredApplicationObject item : data_)
    {
      try
      {
        consumerManager.consume(item, trace, allegroApi_);
      }
      catch (RetryableConsumerException | FatalConsumerException e)
      {
        consumerManager.getUnprocessableMessageConsumer().consume(item, trace,
            "Failed to process message", e);
      }
    }
  }

  @Override
  public String getAfter()
  {
    return after_;
  }

  @Override
  public String getBefore()
  {
    return before_;
  }

  @Override
  public List<IStoredApplicationObject> getData()
  {
    return data_;
  }

}
