/*
 *
 *
 * Copyright 2021 Symphony Communication Services, LLC.
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

import java.time.Instant;
import java.util.List;

import com.symphony.oss.canon.runtime.EntityBuilder;
import com.symphony.oss.commons.dom.json.ImmutableJsonObject;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.core.canon.HashType;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.SortKey;
import com.symphony.oss.models.object.canon.facade.StoredApplicationObject;

/**
 * Super class for ApplicationObject builders which take an already encrypted payload.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The concrete type for fluent methods.
 */
abstract class BaseEncryptedApplicationObjectBuilder<T extends BaseEncryptedApplicationObjectBuilder<T>> extends EntityBuilder<T, IStoredApplicationObject>
{
  protected final StoredApplicationObject.Builder  builder_ = new StoredApplicationObject.Builder();
  protected final PodAndUserId ownerId_;
  
  BaseEncryptedApplicationObjectBuilder(Class<T> type, PodAndUserId ownerId)
  {
    super(type);
    ownerId_ = ownerId;
  }
  
  BaseEncryptedApplicationObjectBuilder(Class<T> type, PodAndUserId ownerId,
      IStoredApplicationObject existing)
  {
    super(type);
    ownerId_ = ownerId;
    
    builder_.withPartitionHash(existing.getPartitionHash())
      .withSortKey(existing.getSortKey())
      .withOwner(ownerId_)
      .withPurgeDate(existing.getPurgeDate())
      .withBaseHash(existing.getBaseHash())
      .withPrevHash(existing.getAbsoluteHash())
      .withPrevSortKey(existing.getSortKey())
      ;
  }

  /**
   * Set the unencrypted header for this object.
   * 
   * @param header The unencrypted header for this object.
   * 
   * @return This (fluent method).
   */
  public T withHeader(IApplicationObjectHeader header)
  {
    builder_.withHeader(header);
    
    return self();
  }

  /**
   * Set the purge date for this object.
   * 
   * @param purgeDate The date after which this object may be deleted by the system.
   * 
   * @return This (fluent method).
   */
  public T withPurgeDate(Instant purgeDate)
  {
    builder_.withPurgeDate(purgeDate);
    
    return self();
  }
  
  @Override
  public ImmutableJsonObject getJsonObject()
  {
    return builder_.getJsonObject();
  }

  @Override
  public String getCanonType()
  {
    return builder_.getCanonType();
  }

  @Override
  public Integer getCanonMajorVersion()
  {
    return builder_.getCanonMajorVersion();
  }

  @Override
  public Integer getCanonMinorVersion()
  {
    return builder_.getCanonMinorVersion();
  }

  @Override
  protected void populateAllFields(List<Object> result)
  {
    builder_.populateAllFields(result);
  }
  
  /**
   * Set the sort key for the object.
   * 
   * @param sortKey The sort key to be attached to this object within its partition.
   * 
   * @return This (fluent method).
   */
  public T withSortKey(SortKey sortKey)
  {
    builder_.withSortKey(sortKey);
    
    return self();
  }
  
  /**
   * Set the sort key for the object.
   * 
   * @param sortKey The sort key to be attached to this object within its partition.
   * 
   * @return This (fluent method).
   */
  public T withSortKey(String sortKey)
  {
    builder_.withSortKey(sortKey);
    
    return self();
  }
  
  @Override
  protected void validate()
  {
    if(builder_.getHashType() == null)
      builder_.withHashType(HashType.newBuilder().build(Hash.getDefaultHashTypeId()));

    builder_.withOwner(ownerId_);
    
    super.validate();
  }
}