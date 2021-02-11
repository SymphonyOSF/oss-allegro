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

import javax.annotation.Nullable;

import com.symphony.oss.allegro.api.request.VersionQuery;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IPageOfAbstractStoredApplicationObject;

class ObjectVersionPage extends AbstractObjectPage<IAbstractStoredApplicationObject> implements IObjectVersionPage
{
  private final VersionQuery query_;

  public ObjectVersionPage(IAllegroMultiTenantApi allegroApi, VersionQuery query, IPageOfAbstractStoredApplicationObject page)
  {
    super(allegroApi, page.getPagination(), Boolean.TRUE == query.getScanForwards(), page.getData());

    query_ = query;
  }

  public VersionQuery getQuery()
  {
    return query_;
  }
  
  public Hash getBaseHash()
  {
    return query_.getBaseHash();
  }

  @Override
  public @Nullable IObjectVersionPage  fetchNextPage()
  {
    if(after_ == null)
      return null;
    
    return allegroApi_.fetchObjectVersionsPage(new VersionQuery.Builder()
        .withAfter(after_)
        .withBaseHash(getBaseHash())
        .withMaxItems(query_.getMaxItems())
        .withScanForwards(true)
        .build()
        );
  }
  
  @Override
  public @Nullable IObjectVersionPage  fetchPrevPage()
  {
    if(before_ == null)
      return null;
    
    return allegroApi_.fetchObjectVersionsPage(new VersionQuery.Builder()
        .withAfter(before_)
        .withBaseHash(getBaseHash())
        .withMaxItems(query_.getMaxItems())
        .withScanForwards(false)
        .build()
        );
  }
}
