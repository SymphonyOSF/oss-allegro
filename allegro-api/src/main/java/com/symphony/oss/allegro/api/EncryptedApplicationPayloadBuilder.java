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

import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.object.canon.EncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;

/**
 * Builder for an EncryptedApplicationPayload.
 * 
 * @author Bruce Skingle
 *
 */
public class EncryptedApplicationPayloadBuilder extends BaseEncryptedApplicationPayloadBuilder<EncryptedApplicationPayloadBuilder, IEncryptedApplicationPayload, EncryptedApplicationPayload.Builder>
{
  EncryptedApplicationPayloadBuilder(IAllegroApi cryptoClient)
  {
    super(EncryptedApplicationPayloadBuilder.class, new EncryptedApplicationPayload.Builder(), cryptoClient);
  }

  /**
   * Set the id of the thread with whose content key this object will be encrypted.
   * 
   * @param threadId The id of the thread with whose content key this object will be encrypted.
   * 
   * @return This (fluent method).
   */
  public EncryptedApplicationPayloadBuilder withThreadId(ThreadId threadId)
  {
    builder_.withThreadId(threadId);
    
    return self();
  }

  @Override
  protected IEncryptedApplicationPayload construct()
  {
    return builder_.build();
  }
}