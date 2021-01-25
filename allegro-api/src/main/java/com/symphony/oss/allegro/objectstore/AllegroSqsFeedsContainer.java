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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.symphony.oss.allegro.api.request.FeedId;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.object.canon.IFeedsEndpoint;

/**
 * @author Geremia Longobardo
 * 
 * Class used to refresh Feed Read Records and check if direct fetch is enabled
 */
public class AllegroSqsFeedsContainer
{
    private String            endpoint_;
    private boolean           directFetch_;
    private List<FeedId>      feedIds_;
    private List<String>      stringIds_;
    private Instant           expiryDate_ = Instant.now().minusMillis(1000 * 60 * 30);
    private AllegroBaseApi  allegro_;

    private static final long      threshold = 40000;
    
    /**
     * @param feedIds The ids of the feeds
     * @param owner   The feeds owner
     * @param allegro The Allegro API object
     */
    public AllegroSqsFeedsContainer(List<FeedId> feedIds, PodAndUserId owner, AllegroBaseApi allegro) 
    {
      feedIds_ = feedIds;
      allegro_ = allegro;
      
      
      stringIds_ = new ArrayList<>();
      
      for(FeedId feedId : feedIds_) 
        stringIds_.add(feedId.getHash(owner).toStringBase64());
      
      refresh();
    }

    /**
     * @return the ids of the feeds.
     */
    public List<String> getFeedIds()
    {
      return stringIds_;
    }
    
    /**
     *  Refreshes the Feed Read Records each 15 minutes
     */
    public void refresh()
    {
      synchronized(this) 
      {
        if(isExpired()) 
        {
          IFeedsEndpoint  tc    =       allegro_.refreshFeeds(feedIds_);
          directFetch_                  = tc.getDirectFetch();
          endpoint_                     = tc.getEndPoint();
          expiryDate_                   = tc.getExpirationDate();
        }
      }
    }
    
    /**
     * @return true if the Feed Read Records need to be refreshed
     */
    public boolean isExpired()
    {
      return expiryDate_.toEpochMilli() - Instant.now().toEpochMilli() < threshold;
    }

    /**
     * @return The endpoint Path
     */
    public String getEndpoint()
    {
      return endpoint_;
    }

    /**
     * @return the AllegroAPI
     */
    public IBaseObjectStoreApi getAllegro()
    {
      return allegro_;
    }
    
    /**
     * @return true if direct fetch is enabled
     */
    public boolean isDirect() 
    {
      return directFetch_;
    }
}
