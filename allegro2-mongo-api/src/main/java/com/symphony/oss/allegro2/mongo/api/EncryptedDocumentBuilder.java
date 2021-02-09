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

package com.symphony.oss.allegro2.mongo.api;

import org.bson.Document;

import com.symphony.oss.allegro2.api.ApplicationRecordBuilder;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.core.canon.facade.IEncryptedApplicationRecord;
import com.symphony.oss.models.core.canon.facade.ThreadId;

public class EncryptedDocumentBuilder extends BaseAbstractBuilder<EncryptedDocumentBuilder, Document>
{
  protected final ApplicationRecordBuilder builder_;

  EncryptedDocumentBuilder(ApplicationRecordBuilder builder)
  {
    super(EncryptedDocumentBuilder.class);
    
    builder_ = builder;
  }

  /**
   * Set the id of the thread with whose content key this object will be encrypted.
   * 
   * @param threadId The id of the thread with whose content key this object will be encrypted.
   * 
   * @return This (fluent method).
   */
  public EncryptedDocumentBuilder withThreadId(ThreadId threadId)
  {
    builder_.withThreadId(threadId);
    
    return self();
  }

  public ThreadId getThreadId()
  {
    return builder_.getThreadId();
  }

  /**
   * Set the object payload (which is to be encrypted).
   * 
   * @param payload The object payload (which is to be encrypted).
   * 
   * @return This (fluent method).
   */
  public EncryptedDocumentBuilder withPayload(IApplicationPayload payload)
  {
    builder_.withPayload(payload);
    
    return self();
  }

  /**
   * Set the object payload (which is to be encrypted).
   * 
   * @param payload The object payload (which is to be encrypted).
   * 
   * @return This (fluent method).
   */
  public EncryptedDocumentBuilder withPayload(String payload)
  {
    builder_.withPayload(payload);
    
    return self();
  }

  /**
   * Set the unencrypted header for this object.
   * 
   * @param header The unencrypted header for this object.
   * 
   * @return This (fluent method).
   */
  public EncryptedDocumentBuilder withHeader(IApplicationPayload header)
  {
    builder_.withHeader(header);
    
    return self();
  }

  /**
   * Set the unencrypted header for this object.
   * 
   * @param header The unencrypted header for this object.
   * 
   * @return This (fluent method).
   */
  public EncryptedDocumentBuilder withHeader(String header)
  {
    builder_.withHeader(header);
    
    return self();
  }

  @Override
  protected Document construct()
  {
    IEncryptedApplicationRecord applicationRecord = builder_.build();
    
    return Document.parse(applicationRecord.toString());
  }
}