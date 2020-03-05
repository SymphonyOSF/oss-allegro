/*
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.symphony.oss.models.chat.canon.ICryptoRotationInfo;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.internal.pod.canon.ICryptoRotationInfoResponse;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;

class ThreadRotationIdCache
{
  private final CloseableHttpClient        httpclient_;
  private final PodInternalHttpModelClient podInternalApiClient_;

  private final LoadingCache<ThreadId, RotationId>  rotationIdCache_ = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(60, TimeUnit.MINUTES)
      .build(
          new CacheLoader<ThreadId, RotationId>()
          {
            @Override
            public RotationId load(ThreadId threadId)
            {
              return fetchRotationId(threadId);
            }
          });

  public ThreadRotationIdCache(CloseableHttpClient httpclient, PodInternalHttpModelClient podInternalApiClient)
  {
    httpclient_ = httpclient;
    podInternalApiClient_ = podInternalApiClient;
  }

  RotationId getRotationId(ThreadId threadId)
  { 
    try
    {
      return rotationIdCache_.get(threadId);
    }
    catch (ExecutionException e)
    {
      throw new IllegalStateException(e);
    }
  }

  private RotationId fetchRotationId(ThreadId threadId)
  {
    // The result of this method is required so .get(0) is guaranteed to succeed below (an exception would be thrown from execute in that case).
    
    ICryptoRotationInfoResponse rotationInfo = podInternalApiClient_.newKeystoreCryptoRotationInfoMultiplePostHttpRequestBuilder()
        .withCanonPayload(threadId)
        .build()
        .execute(httpclient_);
    
    ICryptoRotationInfo info = rotationInfo
        .getData().get(0)
        .getCryptoRotationInfo();
    
    if(info == null)
      return null; //TODO: fix this
    
    return info
        .getAcceptedRotationId();
  }
}
