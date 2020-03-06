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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.symphonyoss.s2.common.fault.FaultAccumulator;

import com.symphony.oss.allegro.api.request.FetchPartitionObjectsRequest;
import com.symphony.oss.allegro.api.request.PartitionQuery;

/**
 * AbstractAsyncQueryListManager implementation for Partition query.
 *   
 * @author Bruce Skingle
 *
 */
public class AsyncPartitionQueryListManager extends AbstractAsyncQueryListManager<AsyncPartitionQueryListManager, AsyncPartitionQueryManager>
{
  protected AsyncPartitionQueryListManager(Builder builder)
  {
    super(AsyncPartitionQueryListManager.class, builder);
  }
  
  public static class Builder extends AbstractBuilder<Builder, AsyncPartitionQueryListManager>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected AsyncPartitionQueryListManager construct()
    {
      return new AsyncPartitionQueryListManager(this);
    }
  }
  
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractAsyncQueryListManager<B, AsyncPartitionQueryManager>> extends AbstractAsyncQueryListManager.AbstractBuilder<T, AsyncPartitionQueryManager, B>
  {
    protected FetchPartitionObjectsRequest  request_;

    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    /**
     * Set the request details.
     * 
     * @param request The request details.
     * 
     * @return This (fluent method).
     */
    public T withRequest(FetchPartitionObjectsRequest request)
    {
      request_ = request;
      return self();
    }

    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(request_,     "Request");
    }
    

    @Override
    protected List<AsyncPartitionQueryManager> createQueryManagers( ThreadPoolExecutor handlerExecutor)
    {
      List<AsyncPartitionQueryManager> queryManagers = new LinkedList<>();
      
      for(PartitionQuery query : request_.getQueryList())
      {
        queryManagers.add(new AsyncPartitionQueryManager(allegroApi_, query, consumerManager_, traceFactory_, objectApiClient_,
            httpClient_, handlerExecutor));
      }
      
      return queryManagers;
    }

    @Override
    protected int getQueryCount()
    {
      return request_.getQueryList().size();
    }
  }
}

