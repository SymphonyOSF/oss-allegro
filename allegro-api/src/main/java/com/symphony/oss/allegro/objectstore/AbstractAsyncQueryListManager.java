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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.symphony.oss.commons.concurrent.NamedThreadFactory;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.fugue.Fugue;
import com.symphony.oss.fugue.FugueLifecycleComponent;
import com.symphony.oss.fugue.FugueLifecycleState;
import com.symphony.oss.fugue.trace.ITraceContextTransactionFactory;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;

/**
 * @author Bruce Skingle
 *
 */
public abstract class AbstractAsyncQueryListManager<T extends AbstractAsyncQueryListManager<T,Q>, Q extends AbstractAsyncQueryManager>
  extends FugueLifecycleComponent
  implements IAllegroQueryManager
{
  private static final Logger log_ = LoggerFactory.getLogger(AbstractAsyncQueryListManager.class);

  private final AsyncConsumerManager            consumerManager_;
  private final int                             subscriberThreadPoolSize_;
  private final int                             handlerThreadPoolSize_;
  private final LinkedBlockingQueue<Runnable>   executorQueue_ = new LinkedBlockingQueue<Runnable>();
  private final LinkedBlockingQueue<Runnable>   handlerQueue_  = new LinkedBlockingQueue<Runnable>();
  private final ImmutableList<Q>                queryManagers_;
  
  private ThreadPoolExecutor                    subscriberExecutor_;
  private ThreadPoolExecutor                    handlerExecutor_;

  private List<Q> remainingQueryManagers_;

  protected AbstractAsyncQueryListManager(AbstractBuilder<?,Q,T> builder)
  {
    super(builder);

    consumerManager_          = builder.consumerManager_;
    subscriberThreadPoolSize_ = Fugue.isDebugSingleThread() ? 1 : consumerManager_.getSubscriberThreadPoolSize() == null ? builder.getQueryCount() : consumerManager_.getSubscriberThreadPoolSize();
    handlerThreadPoolSize_    = Fugue.isDebugSingleThread() ? 1 : consumerManager_.getHandlerThreadPoolSize() == null ? 9 * subscriberThreadPoolSize_ : consumerManager_.getHandlerThreadPoolSize();
   
    log_.info("AbstractAsyncQueryListManager has " + subscriberThreadPoolSize_ +
        " subscriber threads and " + handlerThreadPoolSize_ + " handler threads for a total of " +
        builder.getQueryCount() + " queries...");

    subscriberExecutor_ = new ThreadPoolExecutor(subscriberThreadPoolSize_, subscriberThreadPoolSize_,
        10000L, TimeUnit.MILLISECONDS,
        executorQueue_, new NamedThreadFactory("Query-subscriber"));
    
    handlerExecutor_ = new ThreadPoolExecutor(handlerThreadPoolSize_, handlerThreadPoolSize_,
        10000L, TimeUnit.MILLISECONDS,
        handlerQueue_, new NamedThreadFactory("Query-handler", true));
    
    remainingQueryManagers_ = builder.createQueryManagers(handlerExecutor_);
    queryManagers_ = ImmutableList.copyOf(remainingQueryManagers_);
  }
  
  @Override
  public boolean isIdle()
  {
    synchronized(remainingQueryManagers_)
    {
      return remainingQueryManagers_.isEmpty();
    }
  }

  @Override
  public void waitUntilIdle() throws InterruptedException
  {
    synchronized(remainingQueryManagers_)
    {
      while(!remainingQueryManagers_.isEmpty())
        remainingQueryManagers_.wait();
    }
  }

  @Override
  public void start()
  {
    setLifeCycleState(FugueLifecycleState.Starting);
    
    log_.info("Starting AbstractAsyncQueryListManager with " + subscriberThreadPoolSize_ +
        " subscriber threads and " + handlerThreadPoolSize_ + " handler threads for a total of " +
        queryManagers_.size() + " queries...");
     
    for(Q queryManager : queryManagers_)
    {
      //log_.info("Starting subscription to " + queryManager.getFeedHash() + "...");
      subscriberExecutor_.submit(() -> 
      {
        try
        {
          queryManager.run();
        }
        catch(RuntimeException e)
        {
          log_.error("Failed to process query", e);
        }
        
        try
        {
          queryManager.waitUntilIdle();
        }
        catch (InterruptedException e)
        {
          throw new IllegalStateException("Interrupted", e);
        }
        synchronized(remainingQueryManagers_)
        {
          if(remainingQueryManagers_.remove(queryManager))
            remainingQueryManagers_.notifyAll();
        }
      });
    }
    
    setLifeCycleState(FugueLifecycleState.Running);
  }

  @Override
  public void stop()
  {
    for(Q queryManager : queryManagers_)
    {
      queryManager.stop();
    }
    
    if(subscriberExecutor_ != null)
      subscriberExecutor_.shutdown();
    
    if(handlerExecutor_ != null)
      handlerExecutor_.shutdown();
      
    if(subscriberExecutor_ != null)
      stop(subscriberExecutor_, 60);
    
    if(handlerExecutor_ != null)
      stop(handlerExecutor_, 10);
    

    consumerManager_.closeConsumers();
  }

  private void stop(ThreadPoolExecutor executor, int delay)
  {
    try {
      // Wait a while for existing tasks to terminate
      if (!executor.awaitTermination(delay, TimeUnit.SECONDS)) {
        executor.shutdownNow(); // Cancel currently executing tasks
      // Wait a while for tasks to respond to being cancelled
      if (!executor.awaitTermination(delay, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
        }
      } catch (InterruptedException ie) {
        // (Re-)Cancel if current thread also interrupted
      executor.shutdownNow();
      // Preserve interrupt status
        Thread.currentThread().interrupt();
    }
  }

//  protected void submit(Runnable subscriber, boolean force)
//  {
//    if(force || executorQueue_.size() < subscriberThreadPoolSize_)
//      subscriberExecutor_.submit(subscriber);
//  }
//
//  protected void printQueueSize()
//  {
//    log_.debug("Queue size " + executorQueue_.size());
//  }
//
//  ThreadPoolExecutor getHandlerExecutor()
//  {
//    return handlerExecutor_;
//  }
//
//  ThreadPoolExecutor getSubscriberExecutor()
//  {
//    return subscriberExecutor_;
//  }
  
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,Q,B>, 
    Q extends AbstractAsyncQueryManager, 
    B extends AbstractAsyncQueryListManager<B,Q>> extends FugueLifecycleComponent.AbstractBuilder<T,B>
  {
    protected AllegroBaseApi                  allegroApi_;
    protected ITraceContextTransactionFactory traceFactory_;
    protected ObjectHttpModelClient           objectApiClient_;
    protected CloseableHttpClient             httpClient_;
    protected AsyncConsumerManager            consumerManager_;
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    /**
     * Pass a reference to IAllegroApi.
     * 
     * @param allegroApi to IAllegroApi.
     * 
     * @return This (fluent method).
     */
    public T withAllegroApi(AllegroBaseApi allegroApi)
    {
      allegroApi_ = allegroApi;
      
      return self();
    }

    /**
     * Set the trace context transaction factory to be used.
     * 
     * @param traceFactory The trace context transaction factory to be used.
     * 
     * @return this (fluent method)
     */
    public T withTraceContextTransactionFactory(ITraceContextTransactionFactory traceFactory)
    {
      traceFactory_ = traceFactory;
      
      return self();
    }

    /**
     * Set the Object store client to use.
     * 
     * @param objectApiClient The Object store client to use.
     * 
     * @return This (fluent method).
     */
    public T withObjectApiClient(ObjectHttpModelClient objectApiClient)
    {
      objectApiClient_ = objectApiClient;
      
      return self();
    }

    /**
     * Set the HTTP client to use.
     * 
     * @param httpClient The HTTP client to use.
     * 
     * @return This (fluent method).
     */
    public T withHttpClient(CloseableHttpClient httpClient)
    {
      httpClient_ = httpClient;
      
      return self();
    }
    
    /**
     * Set the consumer manager.
     * 
     * @param consumerManager The consumer manager.
     * 
     * @return This (fluent method).
     */
    public T withConsumerManager(AsyncConsumerManager consumerManager)
    {
      consumerManager_ = consumerManager;
      
      return self();
    }

    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkNotNull(allegroApi_,      "AllegroApi");
      faultAccumulator.checkNotNull(consumerManager_, "ConsumerManager");
    }

    protected abstract List<Q> createQueryManagers(ThreadPoolExecutor handlerExecutor);
    protected abstract int     getQueryCount();
  }
}
