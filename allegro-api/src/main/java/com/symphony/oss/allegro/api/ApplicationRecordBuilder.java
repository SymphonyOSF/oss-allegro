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

import java.util.List;

import com.symphony.oss.commons.dom.json.ImmutableJsonObject;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationRecord;
import com.symphony.oss.models.object.canon.facade.StoredApplicationRecord;

public class ApplicationRecordBuilder extends EncryptablePayloadbuilder<ApplicationRecordBuilder, IStoredApplicationRecord>
{
  protected final StoredApplicationRecord.Builder  builder_ = new StoredApplicationRecord.Builder();
  protected final AllegroCryptoClient cryptoClient_;
  
  ApplicationRecordBuilder(AllegroCryptoClient cryptoClient)
  {
    super(ApplicationRecordBuilder.class);
    
    cryptoClient_ = cryptoClient;
  }

  /**
   * Set the id of the thread with whose content key this object will be encrypted.
   * 
   * @param threadId The id of the thread with whose content key this object will be encrypted.
   * 
   * @return This (fluent method).
   */
  public ApplicationRecordBuilder withThreadId(ThreadId threadId)
  {
    builder_.withThreadId(threadId);
    
    return self();
  }

  @Override
  public ThreadId getThreadId()
  {
    return builder_.getThreadId();
  }

  @Override
  ApplicationRecordBuilder withEncryptedPayload(
      EncryptedData value)
  {
    builder_.withEncryptedPayload(value);
    
    return self();
  }

  @Override
  ApplicationRecordBuilder withCipherSuiteId(
      CipherSuiteId value)
  {
    builder_.withCipherSuiteId(value);
    
    return self();
  }

  @Override
  ApplicationRecordBuilder withRotationId(RotationId value)
  {
    builder_.withRotationId(value);
    
    return self();
  }

  /**
   * Set the object payload (which is to be encrypted).
   * 
   * @param payload The object payload (which is to be encrypted).
   * 
   * @return This (fluent method).
   */
  public ApplicationRecordBuilder withPayload(IApplicationObjectPayload payload)
  {
    payload_ = payload;
    
    return self();
  }

  /**
   * Set the unencrypted header for this object.
   * 
   * @param header The unencrypted header for this object.
   * 
   * @return This (fluent method).
   */
  public ApplicationRecordBuilder withHeader(IApplicationObjectHeader header)
  {
    builder_.withHeader(header);
    
    return self();
  }

//  /**
//   * Set the purge date for this object.
//   * 
//   * @param purgeDate The date after which this object may be deleted by the system.
//   * 
//   * @return This (fluent method).
//   */
//  public ApplicationRecordBuilder withPurgeDate(Instant purgeDate)
//  {
//    builder_.withPurgeDate(purgeDate);
//    
//    return self();
//  }
  
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
  
  @Override
  protected void validate()
  {
    if(getThreadId() == null)
    {
      if(payload_ != null || builder_.getEncryptedPayload() != null)
        throw new IllegalStateException("ThreadId is required unless there is no payload.");
    }
    else
    {
      if(payload_ == null)
      {
        if(builder_.getEncryptedPayload() == null)
          throw new IllegalStateException("One of Payload or EncryptedPayload is required.");
      }
      else
      {
        cryptoClient_.encrypt(this);
      }
    }
    
    super.validate();
  }
  

  @Override
  protected IStoredApplicationRecord construct()
  {
    return builder_.build();
  }
}