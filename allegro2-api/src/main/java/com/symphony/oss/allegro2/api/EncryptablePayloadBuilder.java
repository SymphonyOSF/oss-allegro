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

package com.symphony.oss.allegro2.api;

import com.symphony.oss.canon.runtime.EntityBuilder;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.commons.immutable.ImmutableByteArray;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.EncryptedData;

/**
 * Base class of application objects which can be encrypted.
 * 
 * This is a type expected by AllegroCryptoClient.encrypt(EncryptablePayloadBuilder2),
 * I would have made this an interface but I want some methods to be non-public.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The concrete type for fluent methods.
 * @param <B> The concrete type of the built type.
 */
public abstract class EncryptablePayloadBuilder<T extends EncryptablePayloadBuilder<T,B>, B extends IEntity> extends EntityBuilder<T, B>
{
  /**
   * Constructor.
   * 
   * @param type The concrete type for fluent methods.
   */
  public EncryptablePayloadBuilder(Class<T> type)
  {
    super(type);
  }

  /**
   * 
   * @return the unencrypted payload.
   */
  protected abstract ImmutableByteArray getPayload();

  /**
   * Return the threadId to be used to encrypt the payload.
   * 
   * @return the threadId to be used to encrypt the payload.
   */
  public abstract ThreadId getThreadId();

  protected abstract T withEncryptedPayload(EncryptedData value);

  protected abstract T withCipherSuiteId(CipherSuiteId value);

  protected abstract T withRotationId(RotationId value);
}