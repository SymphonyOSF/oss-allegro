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

import com.symphony.oss.allegro.api.request.FetchObjectVersionsRequest;
import com.symphony.oss.allegro.api.request.VersionQuery;
import com.symphony.oss.commons.fault.FaultAccumulator;

/**
 * AbstractAsyncQueryListManager implementation for ObjectVersion query.
 *   
 * @author Bruce Skingle
 *
 */
public class AsyncVersionQueryListManager extends AbstractAsyncQueryListManager<AsyncVersionQueryListManager, AsyncVersionQueryManager>
{
  protected AsyncVersionQueryListManager(Builder builder)
  {
    super(builder);
  }
  
  /**
   * The concrete builder
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, AsyncVersionQueryListManager>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected AsyncVersionQueryListManager construct()
    {
      return new AsyncVersionQueryListManager(this);
    }
  }
  
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractAsyncQueryListManager<B, AsyncVersionQueryManager>> extends AbstractAsyncQueryListManager.AbstractBuilder<T, AsyncVersionQueryManager, B>
  {
    protected FetchObjectVersionsRequest  request_;

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
    public T withRequest(FetchObjectVersionsRequest request)
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
    protected List<AsyncVersionQueryManager> createQueryManagers( ThreadPoolExecutor handlerExecutor)
    {
      List<AsyncVersionQueryManager> queryManagers = new LinkedList<>();
      
      for(VersionQuery query : request_.getQueryList())
      {
        queryManagers.add(new AsyncVersionQueryManager(allegroApi_, query, consumerManager_, traceFactory_, objectApiClient_,
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

