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

import org.symphonyoss.s2.common.hash.Hash;

/**
 * A request object for the FetchRecentMessages method.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchSequenceRequest extends ConsumerRequest<FetchSequenceRequest>
{
  private Hash    sequenceHash_;
  private Integer maxItems_;
  private String  after_;
  
  public FetchSequenceRequest()
  {
    super(FetchSequenceRequest.class);
  }
  
  public Hash getSequenceHash()
  {
    return sequenceHash_;
  }
  
  public FetchSequenceRequest withSequenceHash(Hash sequenceHash)
  {
    sequenceHash_ = sequenceHash;
    
    return this;
  }
  
  public String getAfter()
  {
    return after_;
  }
  
  public FetchSequenceRequest withAfter(String after)
  {
    after_ = after;
    
    return this;
  }
  
  public Integer getMaxItems()
  {
    return maxItems_;
  }
  
  public FetchSequenceRequest withMaxItems(Integer maxItems)
  {
    maxItems_ = maxItems;
    
    return this;
  }
}
