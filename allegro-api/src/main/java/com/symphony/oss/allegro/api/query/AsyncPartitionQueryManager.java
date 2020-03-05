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

import org.apache.http.impl.client.CloseableHttpClient;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;

import com.symphony.oss.allegro.api.AllegroBaseApi;
import com.symphony.oss.allegro.api.request.AsyncConsumerManager;
import com.symphony.oss.allegro.api.request.PartitionQuery;
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;
import com.symphony.oss.models.object.canon.PartitionsPartitionHashPageGetHttpRequestBuilder;

public class AsyncPartitionQueryManager extends AbstractAsyncQueryManager
{
  private final PartitionQuery                  query_;
  private final ITraceContextTransactionFactory traceFactory_;
  private final ObjectHttpModelClient           objectApiClient_;
  private final CloseableHttpClient             httpClient_;
  private final Hash                            partitionHash_;

  public AsyncPartitionQueryManager(AllegroBaseApi allegroApi, PartitionQuery query, AsyncConsumerManager consumerManager, ITraceContextTransactionFactory traceFactory,
      ObjectHttpModelClient objectApiClient, CloseableHttpClient httpClient, ThreadPoolExecutor handlerExecutor)
  {
    super(allegroApi, query.getMaxItems() == null ? 0 : query.getMaxItems(), consumerManager, handlerExecutor);
    
    query_ = query;
    traceFactory_ = traceFactory;
    objectApiClient_ = objectApiClient;
    httpClient_ = httpClient;
    partitionHash_ = query_.getHash(allegroApi.getUserId());
  }

  @Override
  public void executeQuery()
  {
    if(query_.getMaxItems() != null && getRemainingItems() <= 0)
      return;
    
    String  after         = query_.getAfter();

    try (ITraceContextTransaction traceTransaction = traceFactory_.createTransaction("fetchPartitionObjects",
        partitionHash_.toString()))
    {
      ITraceContext trace = traceTransaction.open();

      do
      {
        PartitionsPartitionHashPageGetHttpRequestBuilder pageRequest = objectApiClient_
            .newPartitionsPartitionHashPageGetHttpRequestBuilder()
              .withPartitionHash(partitionHash_)
              .withAfter(after)
              .withSortKeyPrefix(query_.getSortKeyPrefix())
              .withScanForwards(query_.getScanForwards());

        if (query_.getMaxItems() != null)
          pageRequest.withLimit(getRemainingItems());

        IPageOfStoredApplicationObject page = pageRequest
            .build()
            .execute(httpClient_);

        after = handle(page, trace);
      } while(isRunnable() && after != null && (query_.getMaxItems() == null || getRemainingItems() > 0));
    }
  }
}
