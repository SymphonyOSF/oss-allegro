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

import java.util.HashSet;
import java.util.Set;

import org.symphonyoss.s2.common.hash.Hash;

import com.symphony.oss.models.fundmental.canon.ISequence;
import com.symphony.oss.models.system.canon.FeedType;

/**
 * Request object for UpsertFeed.
 * 
 * @author Bruce Skingle
 *
 */
public class UpsertFeedRequest extends AbstractUpsertFeedRequest<UpsertFeedRequest>
{
  private String   name_;
  
  /**
   * Constructor.
   */
  public UpsertFeedRequest()
  {
    super(UpsertFeedRequest.class);
  }

  /**
   * Set the name of the feed to upsert.
   * 
   * @param name The name of the feed to upsert.
   * 
   * @return This (fluent method).
   */
  public UpsertFeedRequest withName(String name)
  {
    name_ = name;
    
    return self();
  }

  /**
   * 
   * @return The name of the feed to upsert.
   */
  public String getName()
  {
    return name_;
  }
}

class AbstractUpsertFeedRequest<T extends AbstractUpsertFeedRequest<T>> extends AllegroRequest<T>
{
  private FeedType    type_;
  private Set<Hash>   sequences_ = new HashSet<>();
  
  public AbstractUpsertFeedRequest(Class<T> type)
  {
    super(type);
  }
  
  public T withSequences(Hash ...sequences)
  {
    for(Hash sequence : sequences)
      sequences_.add(sequence);
    
    return self();
  }

  public T withSequences(ISequence ...sequences)
  {
    for(ISequence sequence : sequences)
      sequences_.add(sequence.getBaseHash());
    
    return self();
  }
  
  protected Set<Hash> getSequences()
  {
    return sequences_;
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

  @Override
  public void validate()
  {
    super.validate();
    
    if(sequences_.isEmpty())
      throw new IllegalArgumentException("At least one sequence is required.");
  }
}
