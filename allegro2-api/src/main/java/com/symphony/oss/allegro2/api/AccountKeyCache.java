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

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.gs.ti.wpt.lc.security.cryptolib.RSA;
import com.symphony.oss.models.core.canon.CertificateId;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.internal.km.canon.facade.IUserKeys;
import com.symphony.oss.models.internal.pod.canon.IWrappedAccountKeyResponse;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;
import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;
import com.symphony.security.exceptions.SymphonyPEMFormatException;

class AccountKeyCache
{
  private final CloseableHttpClient        httpclient_;
  private final PodInternalHttpModelClient podInternalApiClient_;
  private final IUserKeys                  userKeys_;
  
  private LoadingCache<Key, byte[]>     accountKeyCache_ = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(60, TimeUnit.MINUTES)
      .build(
          new CacheLoader<Key, byte[]>()
          {
            @Override
            public byte[] load(Key key)
            {
              return fetchAccountKey(key.certId_, key.rotationId_, key.userId_);
            }
          });
  
  class Key
  {
    final String        value_;
    final CertificateId certId_;
    final RotationId    rotationId_;
    final PodAndUserId        userId_;
    
    public Key(CertificateId certId, RotationId rotationId, PodAndUserId userId)
    {
      certId_ = certId;
      rotationId_ = rotationId;
      userId_ = userId;
      
      value_ = certId_ + ":" + rotationId_ + ":" + userId_;
    }

    @Override
    public boolean equals(Object anObject)
    {
      return anObject instanceof Key && value_.equals(((Key)anObject).value_);
    }

    @Override
    public int hashCode()
    {
      return value_.hashCode();
    }

    @Override
    public String toString()
    {
      return value_.toString();
    }
    
  }
  
  AccountKeyCache(CloseableHttpClient httpclient, PodInternalHttpModelClient podInternalApiClient,
      IUserKeys userKeys)
  {
    httpclient_ = httpclient;
    podInternalApiClient_ = podInternalApiClient;
    userKeys_ = userKeys;
  }

  byte[] getAccountKey(CertificateId certId, RotationId rotationId, PodAndUserId userId)
  { 
    try
    {
      return accountKeyCache_.get(new Key(certId, rotationId, userId));
    }
    catch (ExecutionException e)
    {
      throw new IllegalStateException(e);
    }
  }

  private byte[] fetchAccountKey(CertificateId certId, RotationId rotationId, PodAndUserId userId)
  {
    IWrappedAccountKeyResponse wrappedAccountKeyResponse = podInternalApiClient_.newWebcontrollerKeystoreWrappedAccountKeysOneGetHttpRequestBuilder()
      .withCertId(certId)
      .withRotationId(rotationId)
      .withUserId(userId)
      .build()
      .execute(httpclient_)
      ;
    
 // Unwrap account key
    byte[] wrappedKey = wrappedAccountKeyResponse.getData().getWrappedAccountKey().getValue().toByteArray();
    
    try
    {
      byte[] ak = RSA.Decrypt(userKeys_.getPrivateKey().getValue(), wrappedKey, RSA.CL_RSA_PKCS1V15_PADDING);
      if (ak.length != 32) {
        throw new IllegalStateException("AK is invalid, expected 32 bytes, found " + ak.length);
      }
      
      return ak;
    }
    catch (UnsupportedEncodingException | SymphonyInputException | SymphonyEncryptionException
        | SymphonyPEMFormatException e)
    {
      throw new IllegalStateException(e);
    }
  }
}
