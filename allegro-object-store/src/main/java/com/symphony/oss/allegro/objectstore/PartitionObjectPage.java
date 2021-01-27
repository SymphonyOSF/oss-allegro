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

import javax.annotation.Nullable;

import com.symphony.oss.allegro.api.request.PartitionQuery;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

class PartitionObjectPage extends AbstractObjectPage<IStoredApplicationObject> implements IObjectPage
{
  private final Hash                           partitionHash_;
  private final PartitionQuery                 query_;

  public PartitionObjectPage(AllegroBaseApi allegroApi, Hash partitionHash, PartitionQuery query, IPageOfStoredApplicationObject page)
  {
    super(allegroApi, page.getPagination(), Boolean.TRUE == query.getScanForwards(), page.getData());

    
    partitionHash_  = partitionHash;
    query_          = query;
  }

  public Hash getPartitionHash()
  {
    return partitionHash_;
  }

  public PartitionQuery getQuery()
  {
    return query_;
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

}
