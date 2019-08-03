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

import com.symphony.oss.models.fundmental.canon.SequenceType;

/**
 * Base class for SequenceMetaData requests.
 * 
 * @param <T> The concrete type returned by fluent methods.
 * 
 * @author Bruce Skingle
 *
 */
public abstract class SequenceMetaDataRequest<T extends SequenceMetaDataRequest<T>> extends AllegroRequest<T>
{
  private Hash          principalBaseHash_;
  private SequenceType  sequenceType_;
  private String        contentType_;
  
  /**
   * Constructor.
   * 
   * @param type The concrete type returned by fluent methods.
   */
  public SequenceMetaDataRequest(Class<T> type)
  {
    super(type);
  }

  /**
   * 
   * @return The principal base hash.
   */
  public Hash getPrincipalBaseHash()
  {
    return principalBaseHash_;
  }
  
  /**
   * Set the principal base hash.
   * 
   * @param principalBaseHash The principal base hash.
   * 
   * @return This (fluent method)
   */
  public T withPrincipalBaseHash(Hash principalBaseHash)
  {
    principalBaseHash_ = principalBaseHash;
    
    return self();
  }
  
  /**
   * 
   * @return The sequence type for the sequence.
   */
  public SequenceType getSequenceType()
  {
    return sequenceType_;
  }
  
  /**
   * Set the sequence type for the sequence.
   * 
   * @param sequenceType The sequence type for the sequence.
   * 
   * @return This (fluent method)
   */
  public T withSequenceType(SequenceType sequenceType)
  {
    sequenceType_ = sequenceType;
    
    return self();
  }
  
  /**
   * 
   * @return The content type for the sequence.
   */
  public String getContentType()
  {
    return contentType_;
  }
  
  /**
   * Set the content type for the sequence.
   * 
   * @param contentType The content type for the sequence.
   * 
   * @return This (fluent method)
   */
  public T withContentType(String contentType)
  {
    contentType_ = contentType;
    
    return self();
  }
  
  @Override
  public void validate()
  {
    super.validate();
    
    require(sequenceType_, "Sequence Type");
    require(contentType_, "Content Type");
  }
}
