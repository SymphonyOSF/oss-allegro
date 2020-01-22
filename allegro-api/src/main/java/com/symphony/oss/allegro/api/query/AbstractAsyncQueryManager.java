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

package com.symphony.oss.allegro.api.query;

import java.util.concurrent.ThreadPoolExecutor;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.request.AsyncConsumerManager;
import com.symphony.oss.models.core.canon.ICursors;
import com.symphony.oss.models.core.canon.IPagination;
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

public abstract class AbstractAsyncQueryManager implements Runnable
{
  private final IAllegroApi          allegroApi_;
  private final AsyncConsumerManager consumerManager_;
  private final ThreadPoolExecutor   handlerExecutor_;

  private int                        remainingItems_;
  private boolean                    runnable_ = true;
  private boolean                    running_  = true;

  protected AbstractAsyncQueryManager(IAllegroApi allegroApi, int remainingItems, AsyncConsumerManager consumerManager,
      ThreadPoolExecutor handlerExecutor)
  {
    allegroApi_ = allegroApi;
    remainingItems_ = remainingItems;
    consumerManager_ = consumerManager;
    handlerExecutor_ = handlerExecutor;
  }

  @Override
  public final void run()
  {
    executeQuery();
      
    synchronized(this)
    {
      running_ = false;
    }
  }

  protected abstract void executeQuery();

  synchronized boolean isRunning()
  {
    return running_;
  }

  protected synchronized boolean isRunnable()
  {
    return runnable_;
  }
  
  protected synchronized int getRemainingItems()
  {
    return remainingItems_;
  }
  
  protected synchronized void stop()
  {
    runnable_ = false;
  }

  protected String handle(IPageOfStoredApplicationObject page, ITraceContext trace)
  {
    for(int i=0 ; i<page.getData().size() ; i++)
    {
      IStoredApplicationObject item = page.getData().get(i);
      
      Runnable task = () -> {
        try
        {
          consumerManager_.consume(item, trace, allegroApi_);
        }
        catch(RuntimeException | RetryableConsumerException | FatalConsumerException e)
        {
          consumerManager_.getUnprocessableMessageConsumer().consume(item, trace,
              "Failed to process message", e);
        }
      };
      
      if(i < page.getData().size() - 1)
      {
        handlerExecutor_.submit(task);
      }
      else
      {
        // Execute the consumer for the last item from the subscriber thread to avoid a backlog developing.
        // I'm not sure is this is needed for a query as opposed to a queue subscriber but I am leaving this code here for now.
        task.run();
      }

      synchronized(this)
      {
        remainingItems_--;
      }
    }

    String after = null;
    IPagination pagination = page.getPagination();

    if (pagination != null)
    {
      ICursors cursors = pagination.getCursors();

      if (cursors != null)
        after = cursors.getAfter();
    }
    
    return after;
  }
}
