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

package com.symphony.oss.allegro.ui;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.symphony.oss.allegro.objectstore.IAllegroObjectStoreApi;
import com.symphony.oss.allegro.objectstore.IBaseObjectStoreApi;
import com.symphony.oss.commons.fault.CodingFault;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;

class PartitionObjectsViewProvider implements Consumer<IAbstractStoredApplicationObject>
{
  private final IBaseObjectStoreApi accessApi_;
  private final IAllegroObjectStoreApi userApi_;
  private final LoadingCache<Hash, CacheEntry<PartitionObjectsView>> partitionCache_ = CacheBuilder.newBuilder()
      .maximumSize(1000)
      //.expireAfterWrite(10, TimeUnit.MINUTES)
      .build(
          new CacheLoader<Hash, CacheEntry<PartitionObjectsView>>()
          {
            @Override
            public CacheEntry<PartitionObjectsView> load(Hash partitionHash)
            {
              try
              {
                return new CacheEntry<PartitionObjectsView>(new PartitionObjectsView(partitionHash, accessApi_, userApi_));
              }
              catch(RuntimeException e)
              {
                return new CacheEntry<PartitionObjectsView>(e);
              }
            }
          });


  PartitionObjectsViewProvider(IBaseObjectStoreApi accessApi, IAllegroObjectStoreApi userApi)
  {
    accessApi_ = accessApi;
    userApi_ = userApi;
  }
  
  PartitionObjectsView getIfPresent(Hash partitionHash)
  {
    CacheEntry<PartitionObjectsView> item = partitionCache_.getIfPresent(partitionHash);
    
    if(item == null)
      return null;
    
    return item.getObject();
  }
  
  PartitionObjectsView fetch(Hash partitionHash)
  {
    try
    {
      return partitionCache_.get(partitionHash).get();
    }
    catch (ExecutionException e)
    {
      throw new CodingFault(e);
    }
  }

  @Override
  public void accept(IAbstractStoredApplicationObject abstractStoredObject)
  {
    CacheEntry<PartitionObjectsView> ce = partitionCache_.getIfPresent(abstractStoredObject.getPartitionHash());
    
    if(ce != null && ce.getObject() != null)
      ce.getObject().accept(abstractStoredObject);
  }
}
