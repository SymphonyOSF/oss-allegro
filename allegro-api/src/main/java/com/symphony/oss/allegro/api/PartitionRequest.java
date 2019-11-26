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

import com.symphony.oss.models.core.canon.facade.PodAndUserId;

/**
 * Base class for SequenceMetaData requests.
 * 
 * @param <T> The concrete type returned by fluent methods.
 * 
 * @author Bruce Skingle
 *
 */
public abstract class PartitionRequest<T extends PartitionRequest<T>> extends AllegroRequest<T>
{
  private PodAndUserId owner_;
  private String       name_;
  
  /**
   * Constructor.
   * 
   * @param type The concrete type returned by fluent methods.
   */
  public PartitionRequest(Class<T> type)
  {
    super(type);
  }

  /**
   * 
   * @return The principal base hash.
   */
  public PodAndUserId getOwner()
  {
    return owner_;
  }
  
  /**
   * Set the principal base hash.
   * 
   * @param owner The principal base hash.
   * 
   * @return This (fluent method)
   */
  public T withOwner(PodAndUserId owner)
  {
    owner_ = owner;
    
    return self();
  }
  
  /**
   * 
   * @return The content type for the sequence.
   */
  public String getName()
  {
    return name_;
  }
  
  /**
   * Set the content type for the sequence.
   * 
   * @param name The content type for the sequence.
   * 
   * @return This (fluent method)
   */
  public T withName(String name)
  {
    name_ = name;
    
    return self();
  }
  
  @Override
  public void validate()
  {
    super.validate();
    
    require(name_, "Name");
  }
}
