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

package com.symphony.oss.allegro.objectstore;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.symphony.oss.fugue.pipeline.FatalConsumerException;
import com.symphony.oss.fugue.pipeline.RetryableConsumerException;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.fugue.trace.NoOpTraceContext;
import com.symphony.oss.models.core.canon.ICursors;
import com.symphony.oss.models.core.canon.IPagination;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;

abstract class AbstractObjectPage<T extends IAbstractStoredApplicationObject> implements IAbstractObjectPage<T>
{
  final AllegroBaseApi allegroApi_;
  final String         after_;
  final String         before_;
  final List<T>        data_;

  AbstractObjectPage(AllegroBaseApi allegroApi, IPagination pagination, boolean scanForwards, List<T> data)
  {
    String after  = null;
    String before = null;
    
    if (pagination != null)
    {
      ICursors cursors = pagination.getCursors();

      if (cursors != null)
      {
        if(scanForwards)
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
    after_          = after;
    before_         = before;
    data_           = initData(scanForwards, data);
  }

  private List<T> initData(boolean scanForwards, List<T> data)
  {
    if(scanForwards)
      return data; // Actually an ImmutableList.;
    
    List<T> list = new ArrayList<>(data.size());
    
    for(int i=data.size() - 1 ; i>=0 ; i--)
    {
      list.add(data.get(i));
    }
    
    return ImmutableList.copyOf(list);
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
  public List<T> getData()
  {
    return data_;
  }
}
