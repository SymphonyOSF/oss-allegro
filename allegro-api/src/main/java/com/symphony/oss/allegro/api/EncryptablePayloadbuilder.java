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

import com.symphony.oss.canon.runtime.EntityBuilder;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;

/**
 * Base class of application objects which can be encrypted.
 * 
 * This is a type expected by AllegroCryptoClient.encrypt(EncryptablePayloadbuilder),
 * I would have made this an interface but I want some methods to be non-public.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The concrete type for fluent methods.
 */
abstract
public class EncryptablePayloadbuilder<T extends EncryptablePayloadbuilder<T,B>, B extends IEntity> extends EntityBuilder<T, B>
{
  protected IApplicationObjectPayload payload_;
  
  EncryptablePayloadbuilder(Class<T> type)
  {
    super(type);
  }

  /**
   * 
   * @return the unencrypted payload.
   */
  public IApplicationObjectPayload getPayload()
  {
    return payload_;
  }

  public abstract ThreadId getThreadId();

  abstract T withEncryptedPayload(EncryptedData value);

  abstract T withCipherSuiteId(CipherSuiteId value);

  abstract T withRotationId(RotationId value);
}