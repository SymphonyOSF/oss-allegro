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

import com.symphony.oss.models.core.canon.facade.ThreadId;

/**
 * Request to create a partition.
 * 
 * @author Bruce Skingle
 *
 */
public class UpsertPartitionRequest extends AllegroRequest<UpsertPartitionRequest>
{
  private String        name_;
  private Set<ThreadId> threadIds_ = new HashSet<>();
  
  /**
   * Constructor.
   */
  public UpsertPartitionRequest()
  {
    super(UpsertPartitionRequest.class);
  }
  
  /**
   * 
   * @return The name of the partition.
   */
  public String getName()
  {
    return name_;
  }
  
  /**
   * Set the name of the partition.
   * 
   * @param name The content type for the sequence.
   * 
   * @return This (fluent method)
   */
  public UpsertPartitionRequest withName(String name)
  {
    name_ = name;
    
    return self();
  }
  
  /**
   * 
   * @return The allowable ThreadIds for this partition.
   */
  public Set<ThreadId> getThreadIds()
  {
    return threadIds_;
  }

  /**
   * Add the given thread IDs to the set of valid threadIds for this partition.
   * 
   * @param threadIds IDs of threads which can be used to encrypt objects in this partition.
   * 
   * @return This (fluent method)
   */
  public UpsertPartitionRequest withThreadIds(ThreadId ...threadIds)
  {
    for(ThreadId threadid : threadIds)
      threadIds_.add(threadid);
    
    return self();
  }
  
  @Override
  public void validate()
  {
    super.validate();
    
    require(name_, "Name");
  }
}
