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

import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayloadAndHeader;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * Builder for application type FundamentalObjects which takes an existing ApplicationObject for which a new
 * version is to be created.
 * 
 * @author Bruce Skingle
 *
 */
public class EncryptedApplicationObjectUpdater extends BaseEncryptedApplicationObjectBuilder<EncryptedApplicationObjectUpdater>
{
  /**
   * Constructor.
   * 
   * @param existing An existing Application Object for which a new version is to be created. 
   */
  public EncryptedApplicationObjectUpdater(PodAndUserId ownerId, IStoredApplicationObject existing)
  {
    super(EncryptedApplicationObjectUpdater.class, ownerId, existing);
    
    builder_
        .withPartitionHash(existing.getPartitionHash())
        .withSortKey(existing.getSortKey())
        .withOwner(ownerId)
        .withThreadId(existing.getThreadId())
        .withHeader(existing.getHeader())
        .withPurgeDate(existing.getPurgeDate())
        .withBaseHash(existing.getBaseHash())
        .withPrevHash(existing.getAbsoluteHash())
        .withPrevSortKey(existing.getSortKey())
        ;
  }
  
  /**
   * Set the already encrypted object payload and header.
   * 
   * @param payload The encrypted object payload and header.
   * 
   * @return This (fluent method).
   */
  public EncryptedApplicationObjectUpdater withEncryptedPayloadAndHeader(IEncryptedApplicationPayloadAndHeader payload)
  {
    withHeader(payload.getHeader());

    return withEncryptedPayload(payload);
  }
  
  /**
   * Set the already encrypted object payload.
   * 
   * @param payload The encrypted object payload.
   * 
   * @return This (fluent method).
   */
  public EncryptedApplicationObjectUpdater withEncryptedPayload(IEncryptedApplicationPayload payload)
  {
    if(!builder_.getThreadId().equals(payload.getThreadId()))
      throw new IllegalArgumentException("The threadId of an object cannot be changed. The object being updated has thread ID " + builder_.getThreadId());
    
    builder_.withEncryptedPayload(payload.getEncryptedPayload());
    builder_.withRotationId(payload.getRotationId());
    builder_.withCipherSuiteId(payload.getCipherSuiteId());
    
    return self();
  }

  @Override
  protected IStoredApplicationObject construct()
  {
    return builder_.build();
  }
}