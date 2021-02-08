/*
 * Copyright 2019 Symphony Communication Services, LLC.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.symphony.oss.models.core.canon.CertificateId;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.crypto.canon.facade.WrappedKey;
import com.symphony.oss.models.internal.km.canon.IWrappedEntityKey;
import com.symphony.oss.models.internal.km.canon.KmInternalHttpModelClient;
import com.symphony.security.exceptions.CiphertextTransportIsEmptyException;
import com.symphony.security.exceptions.CiphertextTransportVersionException;
import com.symphony.security.exceptions.InvalidDataException;
import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;
import com.symphony.security.helper.IClientCryptoHandler;

class EntityKeyCache    
{
  private final CloseableHttpClient                           httpclient_;
  private final KmInternalHttpModelClient                     kmInternalClient_;
  private final AccountKeyCache                               accountKeyCache_;
  private final PodAndUserId                                  internalUserId_;
  private final IClientCryptoHandler                          clientCryptoHandler_;

  private final LoadingCache<RotationId, AllegroCryptoHelper>  contentKeyCache_ = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(60, TimeUnit.MINUTES)
      .build(
          new CacheLoader<RotationId, AllegroCryptoHelper>()
          {
            @Override
            public AllegroCryptoHelper load(RotationId rotationId)
            {
              return fetchEntityKey(rotationId);
            }
          });

  
  

  public EntityKeyCache(CloseableHttpClient httpclient, KmInternalHttpModelClient kmInternalClient,
      AccountKeyCache accountKeyCache, PodAndUserId internalUserId, IClientCryptoHandler clientCryptoHandler)
  {
    httpclient_ = httpclient;
    kmInternalClient_ = kmInternalClient;
    accountKeyCache_ = accountKeyCache;
    internalUserId_ = internalUserId;
    clientCryptoHandler_ = clientCryptoHandler;
  }

  AllegroCryptoHelper getEntityKey()
  { 
    try
    {
      return contentKeyCache_.get(RotationId.ZERO);
    }
    catch (ExecutionException e)
    {
      throw new IllegalStateException(e);
    }
  }

  private AllegroCryptoHelper fetchEntityKey(RotationId rotationId)
  {
    IWrappedEntityKey wrappedEntityKeyResponse = kmInternalClient_.newKeysEntityKeyGetHttpRequestBuilder()
      .build()
      .execute(httpclient_)
      ;
    
    WrappedKey wrappedKey = wrappedEntityKeyResponse.getEntityKey();
    
    byte[] entityKey = unwrapContentKey(wrappedKey.getValue().toByteArray());
    
    return new AllegroCryptoHelper(entityKey, wrappedKey);
  }
  
  private static final RotationId ACCOUNT_KEY_ROTIOTON_ID_THAT_DECRYPTS_ROTATION_0_CONTENT_KEY = RotationId.newBuilder().build(0L);

  /**
   * SymphonyClient has code to push a certificate which seems never to be called. That code could set a different certId....
   */
  private static final CertificateId CERT_ID = CertificateId.newBuilder().build(0L);
  
  private byte[] unwrapContentKey(byte[] wrappedKey)
  {
    try
    {
      byte[] accountKey = accountKeyCache_.getAccountKey(CERT_ID,
          ACCOUNT_KEY_ROTIOTON_ID_THAT_DECRYPTS_ROTATION_0_CONTENT_KEY, internalUserId_);
      
      byte[] entityKey = clientCryptoHandler_.decryptMsg(accountKey, wrappedKey);
      return entityKey;
    }
    catch (SymphonyEncryptionException | SymphonyInputException | InvalidDataException
        | CiphertextTransportIsEmptyException | CiphertextTransportVersionException e)
    {
      throw new IllegalStateException(e);
    }
  }
}
