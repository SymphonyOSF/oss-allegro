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

import com.symphony.oss.models.system.canon.FeedType;

/**
 * Request object for FetchOrCreateFeed.
 * 
 * @author Bruce Skingle
 *
 */
public class UpsertFeedRequest extends AbstractFetchOrCreateFeedRequest<UpsertFeedRequest>
{
  private String   name_;
  
  /**
   * Constructor.
   */
  public UpsertFeedRequest()
  {
    super(UpsertFeedRequest.class);
  }

  public UpsertFeedRequest withName(String name)
  {
    name_ = name;
    
    return self();
  }

  public String getName()
  {
    return name_;
  }
}

class AbstractFetchOrCreateFeedRequest<T extends AbstractFetchOrCreateFeedRequest<T>> extends AllegroRequest<T>
{
  private FeedType type_;
  
  public AbstractFetchOrCreateFeedRequest(Class<T> type)
  {
    super(type);
  }
  
  public T withType(FeedType type)
  {
    type_ = type;
    
    return self();
  }

  public FeedType getType()
  {
    return type_;
  }
}
