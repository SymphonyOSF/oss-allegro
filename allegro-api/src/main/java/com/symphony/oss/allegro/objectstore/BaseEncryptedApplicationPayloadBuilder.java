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

package com.symphony.oss.allegro.objectstore;

import java.util.List;

import com.symphony.oss.allegro.api.EncryptablePayloadbuilder;
import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.commons.dom.json.ImmutableJsonObject;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.object.canon.EncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;

/**
 * Super class for AppplicationObject builder and updater.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The concrete type for fluent methods.
 */
abstract class BaseEncryptedApplicationPayloadBuilder<T extends BaseEncryptedApplicationPayloadBuilder<T,B,P>, B extends IEncryptedApplicationPayload, P extends EncryptedApplicationPayload.AbstractEncryptedApplicationPayloadBuilder<?,?>> extends EncryptablePayloadbuilder<T, B>
{
  protected final P  builder_;
  protected final IAllegroApi cryptoClient_;
  
  BaseEncryptedApplicationPayloadBuilder(Class<T> type, P builder, IAllegroApi cryptoClient)
  {
    super(type);
    builder_ = builder;
    cryptoClient_ = cryptoClient;
  }

  @Override
  public ThreadId getThreadId()
  {
    return builder_.getThreadId();
  }

  @Override
  protected T withEncryptedPayload(
      EncryptedData value)
  {
    builder_.withEncryptedPayload(value);
    
    return self();
  }

  @Override
  protected T withCipherSuiteId(
      CipherSuiteId value)
  {
    builder_.withCipherSuiteId(value);
    
    return self();
  }

  @Override
  protected T withRotationId(RotationId value)
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
  public T withPayload(IApplicationObjectPayload payload)
  {
    payload_ = payload;
    
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
  
  @Override
  protected void validate()
  {
    if(getThreadId() == null)
      throw new IllegalStateException("ThreadId is required.");
    
    if(payload_ == null)
      throw new IllegalStateException("Payload is required.");
    
    cryptoClient_.encrypt(this);
    
    super.validate();
  }
}
