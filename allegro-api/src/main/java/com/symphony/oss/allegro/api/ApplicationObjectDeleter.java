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
import com.symphony.oss.models.object.canon.DeletionType;
import com.symphony.oss.models.object.canon.facade.DeletedApplicationObject;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IDeletedApplicationObject;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.SortKey;

/**
 * Builder for application type FundamentalObjects which takes an existing ApplicationObject for which a new
 * version is to be created.
 * 
 * @author Bruce Skingle
 *
 */
public class ApplicationObjectDeleter extends EntityBuilder<ApplicationObjectDeleter, IDeletedApplicationObject>
{
  private final DeletedApplicationObject.Builder builder_;
  private final PodAndUserId                     ownerId_;
  
  /**
   * Constructor.
   * 
   * @param ownerId The owning user of the object.
   * @param existingObject An existing Application Object for which is to be deleted. 
   */
  public ApplicationObjectDeleter(PodAndUserId ownerId, IApplicationObjectPayload existingObject)
  {
    this(ownerId, existingObject.getStoredApplicationObject());
  }
  
  /**
   * Constructor.
   * 
   * @param existingObject An existing Application Object for which is to be deleted. 
   */
  ApplicationObjectDeleter(PodAndUserId ownerId, IStoredApplicationObject existingObject)
  {
    super(ApplicationObjectDeleter.class, existingObject);
    
    ownerId_ = ownerId;
    
    IStoredApplicationObject existing = existingObject;
    
    builder_ = new DeletedApplicationObject.Builder()
        .withPartitionHash(existing.getPartitionHash())
        .withSortKey(existing.getSortKey())
        .withOwner(ownerId)
        .withPurgeDate(existing.getPurgeDate())
        .withBaseHash(existing.getBaseHash())
        .withPrevHash(existing.getAbsoluteHash())
        .withPrevSortKey(existing.getSortKey())
        ;
  }

  /**
   * Set the deletion type.
   * 
   * @param value The deletion type.
   * 
   * @return This (fluent method).
   */
  public ApplicationObjectDeleter withDeletionType(DeletionType value)
  {
    builder_.withDeletionType(value);
    
    return self();
  }
  
  /**
   * Set the purge date for this object.
   * 
   * This is meaningless in the case of a physical delete but makes sense for a Logical Delete.
   * 
   * @param purgeDate The date after which this object may be deleted by the system.
   * 
   * @return This (fluent method).
   */
  public ApplicationObjectDeleter withPurgeDate(Instant purgeDate)
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
  public ApplicationObjectDeleter withSortKey(SortKey sortKey)
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
  public ApplicationObjectDeleter withSortKey(String sortKey)
  {
    builder_.withSortKey(sortKey);
    
    return self();
  }
  
  @Override
  protected void validate()
  {
    if(builder_.getHashType() == null)
      builder_.withHashType(HashType.newBuilder().build(Hash.getDefaultHashTypeId()));
    
    if(builder_.getDeletionType() == null)
      throw new IllegalStateException("DeletionType is required.");
    
    builder_.withOwner(ownerId_);
    
    super.validate();
  }

  @Override
  protected IDeletedApplicationObject construct()
  {
    return builder_.build();
  }
}
