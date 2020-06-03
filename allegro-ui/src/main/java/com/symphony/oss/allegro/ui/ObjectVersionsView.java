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

import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.IAllegroMultiTenantApi;
import com.symphony.oss.allegro.api.IObjectVersionPage;
import com.symphony.oss.allegro.api.request.VersionQuery;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.IDeletedApplicationObject;

class ObjectVersionsView extends AbstractObjectView<IAbstractStoredApplicationObject>
{
  ObjectVersionsView(Hash baseHash, IAllegroMultiTenantApi accessApi, IAllegroApi userApi)
  {
    super(IAbstractStoredApplicationObject.class, baseHash, accessApi, userApi);
  }

  @Override
  protected IObjectVersionPage fetchPage(IAllegroMultiTenantApi accessApi, Hash hash, String after,
      boolean scanForwards)
  {
    return accessApi.fetchObjectVersionsPage(new VersionQuery.Builder()
        .withBaseHash(hash)
//      .withMaxItems(2)
      .build()
      );
  }
  
  @Override
  void acceptDeletedObject(IDeletedApplicationObject deletedApplicationObject)
  {
    acceptStoredObject(deletedApplicationObject);
  }

  @Override
  Hash getPrimaryKey(IAbstractStoredApplicationObject storedObject)
  {
    return storedObject.getAbsoluteHash();
  }

  @Override
  String getSortKey(IAbstractStoredApplicationObject storedObject)
  {
    return storedObject.getCreatedDate() + "#" + storedObject.getAbsoluteHash();
  }
}
