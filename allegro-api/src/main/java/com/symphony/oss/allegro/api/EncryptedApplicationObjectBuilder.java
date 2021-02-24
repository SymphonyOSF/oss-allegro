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

import com.symphony.oss.allegro.api.request.PartitionId;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayloadAndHeader;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * Builder for Application Objects.
 * 
 * @author Bruce Skingle
 *
 */
public class EncryptedApplicationObjectBuilder extends BaseEncryptedApplicationObjectBuilder<EncryptedApplicationObjectBuilder>
{
  EncryptedApplicationObjectBuilder(PodAndUserId ownerId)
  {
    super(EncryptedApplicationObjectBuilder.class, ownerId);
  }

  /**
   * Set the id of the thread with whose content key this object will be encrypted.
   * 
   * @param threadId The id of the thread with whose content key this object will be encrypted.
   * 
   * @return This (fluent method).
   */
  public EncryptedApplicationObjectBuilder withThreadId(ThreadId threadId)
  {
    builder_.withThreadId(threadId);
    
    return self();
  }
  
  /**
   * Set the partition key for the object from the given partition.
   * 
   * @param partitionHash The Hash of the partition.
   * 
   * @return This (fluent method).
   */
  public EncryptedApplicationObjectBuilder withPartition(Hash partitionHash)
  {
    builder_.withPartitionHash(partitionHash);
    
    return self();
  }
  
  /**
   * Set the partition key for the object from the given partition.
   * 
   * @param partitionId The ID of the partition.
   * 
   * @return This (fluent method).
   */
  public EncryptedApplicationObjectBuilder withPartition(PartitionId partitionId)
  {
    builder_.withPartitionHash(partitionId.getId(ownerId_).getHash());
    
    return self();
  }
  
  /**
   * Set the partition key for the object from the given partition.
   * 
   * @param partition A partition object.
   * 
   * @return This (fluent method).
   */
  public EncryptedApplicationObjectBuilder withPartition(IPartition partition)
  {
    builder_.withPartitionHash(partition.getId().getHash());
    
    return self();
  }
  
  /**
   * Set the already encrypted object payload and header.
   * 
   * @param payload The encrypted object payload and header.
   * 
   * @return This (fluent method).
   */
  public EncryptedApplicationObjectBuilder withEncryptedPayloadAndHeader(IEncryptedApplicationPayloadAndHeader payload)
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
  public EncryptedApplicationObjectBuilder withEncryptedPayload(IEncryptedApplicationPayload payload)
  {
    builder_.withEncryptedPayload(payload.getEncryptedPayload());
    builder_.withRotationId(payload.getRotationId());
    builder_.withCipherSuiteId(payload.getCipherSuiteId());
    builder_.withThreadId(payload.getThreadId());
    
    return self();
  }
  
  @Override
  protected IStoredApplicationObject construct()
  {
    return builder_.build();
  }
}