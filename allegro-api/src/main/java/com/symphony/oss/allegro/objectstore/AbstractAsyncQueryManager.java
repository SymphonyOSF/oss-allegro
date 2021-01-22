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

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.symphony.oss.fugue.pipeline.FatalConsumerException;
import com.symphony.oss.fugue.pipeline.RetryableConsumerException;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.models.core.canon.ICursors;
import com.symphony.oss.models.core.canon.IPagination;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IPageOfAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;

/**
 * @author Bruce Skingle
 *
 */
public abstract class AbstractAsyncQueryManager implements Runnable
{
  private final AllegroDecryptor allegroApi_;
  private final AsyncConsumerManager consumerManager_;
  private final ThreadPoolExecutor   handlerExecutor_;

  private final AtomicInteger        remainingItems_;
  private final AtomicBoolean        runnable_     = new AtomicBoolean(true);
  private final AtomicBoolean        running_      = new AtomicBoolean(true);
  private final AtomicInteger        handlerCount_ = new AtomicInteger(0);

  protected AbstractAsyncQueryManager(AllegroDecryptor allegroApi, int remainingItems, AsyncConsumerManager consumerManager,
      ThreadPoolExecutor handlerExecutor)
  {
    allegroApi_ = allegroApi;
    remainingItems_ = new AtomicInteger(remainingItems);
    consumerManager_ = consumerManager;
    handlerExecutor_ = handlerExecutor;
  }

  @Override
  public final void run()
  {
    executeQuery();
      
    running_.set(false);
  }

  protected abstract void executeQuery();

//  synchronized boolean isRunning()
//  {
//    return running_;
//  }

  protected boolean isRunnable()
  {
    return runnable_.get();
  }
  
  protected int getRemainingItems()
  {
    return remainingItems_.get();
  }
  
  protected void stop()
  {
    runnable_.set(false);
  }

  protected String handle(IPageOfStoredApplicationObject page, ITraceContext trace)
  {
    return handle(page.getData(), page.getPagination(), trace);
  }

  protected String handle(IPageOfAbstractStoredApplicationObject page, ITraceContext trace)
  {
    return handle(page.getData(), page.getPagination(), trace);
  }

  private String handle(List<? extends IAbstractStoredApplicationObject> data, IPagination pagination, ITraceContext trace)
  {
    for(int i=0 ; i<data.size() ; i++)
    {
      IAbstractStoredApplicationObject item = data.get(i);
      
      handlerCount_.incrementAndGet();
      
      Runnable task = () -> {
        try
        {
          consumerManager_.consume(item, trace, allegroApi_);
          
          synchronized(handlerCount_)
          {
            handlerCount_.decrementAndGet();
            handlerCount_.notifyAll();
          }
        }
        catch(RuntimeException | RetryableConsumerException | FatalConsumerException e)
        {
          consumerManager_.getUnprocessableMessageConsumer().consume(item, trace,
              "Failed to process message", e);
        }
      };
      
      if(i < data.size() - 1)
      {
        handlerExecutor_.submit(task);
      }
      else
      {
        // Execute the consumer for the last item from the subscriber thread to avoid a backlog developing.
        // I'm not sure is this is needed for a query as opposed to a queue subscriber but I am leaving this code here for now.
        task.run();
      }

      remainingItems_.decrementAndGet();
    }

    String after = null;

    if (pagination != null)
    {
      ICursors cursors = pagination.getCursors();

      if (cursors != null)
        after = cursors.getAfter();
    }
    
    return after;
  }

  protected void waitUntilIdle() throws InterruptedException
  {
    synchronized(handlerCount_)
    {
      while(handlerCount_.get() > 0)
        handlerCount_.wait();
    }
  }
}
