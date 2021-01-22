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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.allegro.objectstore.IAbstractObjectPage;
import com.symphony.oss.allegro.objectstore.IAllegroApi;
import com.symphony.oss.allegro.objectstore.IAllegroMultiTenantApi;
import com.symphony.oss.allegro.ui.PartitionBlotterPanel.FeedHandler;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IApplicationObjectNotification;
import com.symphony.oss.models.object.canon.facade.IDeletedApplicationObject;

abstract class AbstractObjectView<T extends IAbstractStoredApplicationObject> implements Consumer<IAbstractStoredApplicationObject>
{
  private static final Logger log_ = LoggerFactory.getLogger(AbstractObjectView.class);

  private final Class<T>                                     type_;
  private final Hash                                         hash_;
  private final IAllegroMultiTenantApi                       accessApi_;
  private final IAllegroApi                                  userApi_;
  private Map<Hash, String>                                  hashIndex_ = new HashMap<>();
  private ConcurrentNavigableMap<String, PartitionObject<T>> objectMap_ = new ConcurrentSkipListMap<>();
  private CopyOnWriteArrayList<FeedHandler>                  listeners_ = new CopyOnWriteArrayList<>();

  private String                                             before_;
  private String                                             after_;

  AbstractObjectView(Class<T> type, Hash hash, IAllegroMultiTenantApi accessApi, IAllegroApi userApi)
  {
    type_ = type;
    hash_ = hash;
    accessApi_ = accessApi;
    userApi_ = userApi;
    
    loadPage(null, true);
  }
  
  private void loadPage(String after, boolean scanForwards)
  {
    IAbstractObjectPage<T> page = fetchPage(accessApi_, hash_, after, scanForwards);
        
    addObjects(page);
    
    if(scanForwards)
      after_ = page.getAfter();
    else
      before_ = page.getBefore();
  }

  abstract IAbstractObjectPage<T> fetchPage(IAllegroMultiTenantApi accessApi, Hash hash, String after, boolean scanForwards);

  boolean hasMoreAfter()
  {
    return after_ != null;
  }
  
  void loadAfter()
  {
    if(after_ != null)
    {
      loadPage(after_, true);
    }
  }

  private synchronized void addObjects(IAbstractObjectPage<T> page)
  {
    for(T storedObject : page.getData())
    {
      accept(storedObject);
    }
  }

  void acceptStoredObject(T storedObject)
  {
    String sortKey = getSortKey(storedObject);
    String oldSortKey = hashIndex_.put(getPrimaryKey(storedObject), sortKey );
    
    if(oldSortKey != null && !oldSortKey.equals(sortKey))
    {
      objectMap_.remove(oldSortKey);
      
      for(FeedHandler listener : listeners_)
      {
        listener.objectDeleted(getPrimaryKey(storedObject));
      }
    }
    
    PartitionObject<T> partitionObject = new PartitionObject<>(storedObject, userApi_);
    
    objectMap_.put(sortKey, partitionObject);
    
    for(FeedHandler listener : listeners_)
    {
      listener.objectUpserted(partitionObject);
    }
  }

  abstract String getSortKey(IAbstractStoredApplicationObject storedObject);

  abstract Hash getPrimaryKey(IAbstractStoredApplicationObject storedObject);

  @Override
  public synchronized void accept(IAbstractStoredApplicationObject abstractStoredObject)
  {
    if(abstractStoredObject instanceof IApplicationObjectNotification)
    {
      IApplicationObjectNotification notification = (IApplicationObjectNotification)abstractStoredObject;
      
      abstractStoredObject = accessApi_.fetchAbsolute(notification.getAbsoluteHash());
    }
    
    if(abstractStoredObject instanceof IDeletedApplicationObject)
    {
      acceptDeletedObject((IDeletedApplicationObject)abstractStoredObject);
    }
    else if(type_.isInstance(abstractStoredObject))
    {
      acceptStoredObject(type_.cast(abstractStoredObject));
    }
    else
    {
      log_.error("Unknown message type: " + abstractStoredObject);
    }
  }
  
  void acceptDeletedObject(IDeletedApplicationObject deletedApplicationObject)
  {
    String oldSortKey = hashIndex_.remove(getPrimaryKey(deletedApplicationObject));
    
    if(oldSortKey != null)
    {
      objectMap_.remove(oldSortKey);
      
      for(FeedHandler listener : listeners_)
      {
        listener.objectDeleted(getPrimaryKey(deletedApplicationObject));
      }
    }
  }

  public synchronized void forEach(Consumer<PartitionObject<T>> consumer)
  {
    for(PartitionObject<T> partitionObject : objectMap_.values())
      consumer.accept(partitionObject);
  }

  public void addListener(FeedHandler listener)
  {
    listeners_.add(listener);
  }

  public void removeListener(FeedHandler listener)
  {
    listeners_.remove(listener);
  }
}
