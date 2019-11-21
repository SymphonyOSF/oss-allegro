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

package com.symphony.oss.allegro.api;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;
import org.symphonyoss.s2.canon.runtime.exception.CanonException;
import org.symphonyoss.s2.canon.runtime.exception.NotFoundException;
import org.symphonyoss.s2.canon.runtime.exception.PermissionDeniedException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.gs.ti.wpt.lc.security.cryptolib.AES;
import com.symphony.oss.models.chat.canon.facade.ThreadId;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.crypto.canon.facade.WrappedKey;
import com.symphony.oss.models.fundamental.canon.facade.RotationId;
import com.symphony.oss.models.fundmental.canon.CertificateId;
import com.symphony.oss.models.internal.pod.canon.IWrappedContentKeyResponse;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;
import com.symphony.security.clientsdk.transport.CiphertextFactory;
import com.symphony.security.clientsdk.transport.ICiphertextTransport;
import com.symphony.security.exceptions.CiphertextTransportIsEmptyException;
import com.symphony.security.exceptions.CiphertextTransportVersionException;
import com.symphony.security.exceptions.InvalidDataException;
import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;
import com.symphony.security.helper.KeyIdentifier;
import com.symphony.security.hsm.TransportableWrappedKey;

class ContentKeyCache
{
  private final CloseableHttpClient        httpclient_;
  private final PodInternalHttpModelClient podInternalApiClient_;
  private final AccountKeyCache            accountKeyCache_;
  private final PodAndUserId userId_;

  private final LoadingCache<Key, AllegroCryptoHelper>  contentKeyCache_ = CacheBuilder.newBuilder()
      .maximumSize(1000)
      .expireAfterWrite(60, TimeUnit.MINUTES)
      .build(
          new CacheLoader<Key, AllegroCryptoHelper>()
          {
            @Override
            public AllegroCryptoHelper load(Key key)
            {
              return fetchContentKey(key.threadId_, key.rotationId_, key.userId_);
            }
          });
  
  class Key
  {
    final String        value_;
    final ThreadId      threadId_;
    final RotationId    rotationId_;
    final PodAndUserId        userId_;
    
    public Key(ThreadId threadId, RotationId rotationId, PodAndUserId userId)
    {
      threadId_ = threadId;
      rotationId_ = rotationId;
      userId_ = userId;
      
      value_ = threadId_ + ":" + rotationId_ + ":" + userId_;
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

  public ContentKeyCache(CloseableHttpClient httpclient, PodInternalHttpModelClient podInternalApiClient,
      AccountKeyCache accountKeyCache, PodAndUserId userId)
  {
    httpclient_ = httpclient;
    podInternalApiClient_ = podInternalApiClient;
    accountKeyCache_ = accountKeyCache;
    userId_ = userId;
  }

  AllegroCryptoHelper getContentKey(ThreadId threadId, RotationId rotationId, PodAndUserId userId)
  { 
    try
    {
      return contentKeyCache_.get(new Key(threadId, rotationId, userId));
    }
    catch (ExecutionException e)
    {
      throw new IllegalStateException(e);
    }
    catch(UncheckedExecutionException e)
    {
      if(e.getCause() instanceof CanonException)
        throw (CanonException)e.getCause();
      
      throw e;
    }
  }

  private AllegroCryptoHelper fetchContentKey(ThreadId threadId, RotationId rotationId, PodAndUserId userId)
  {
    KeyIdentifier keyId = new KeyIdentifier(threadId.getValue().toByteArray(), userId.getValue(), rotationId.getValue(), null);
    IWrappedContentKeyResponse wrappedContentKeyResponse;
    
    try
    {
      wrappedContentKeyResponse = podInternalApiClient_.newKeystoreWrappedContentKeysOneGetHttpRequestBuilder()
        .withThreadId(threadId)
        .withRotationId(rotationId)
        .withUserId(userId)
        .build()
        .execute(httpclient_)
        ;
    }
    catch(NotFoundException e)
    {
      throw new PermissionDeniedException("Not a member of this thread", e);
    }
    
    WrappedKey wrappedKey = wrappedContentKeyResponse.getData().getWrappedContentKey();
    
    try
    {
      byte[] contentKey = unwrapContentKey(keyId, wrappedKey.getValue().toByteArray());
      
      return new AllegroCryptoHelper(contentKey, wrappedKey);
    }
    catch (SymphonyInputException | SymphonyEncryptionException e)
    {
      throw new IllegalStateException(e);
    }
  }
  
  
  
  
  
  
  
  
  /*
   * 
   * 
   * "Tainted" code copied from or based on SymphonyClient.
   * 
   * 
   */

  // For deprecated API's,
  private static final long DEFAULT_ROTATION_ID = 0;
  private static final RotationId ACCOUNT_KEY_ROTIOTON_ID_THAT_DECRYPTS_ROTATION_0_CONTENT_KEY = RotationId.newBuilder().build(0L);
  
  /**
   * SymphonyClient has code to push a certificate which seems never to be called. That code could set a different certId....
   */
  private static final CertificateId CERT_ID = CertificateId.newBuilder().build(0L);
  
  public byte[] unwrapContentKey(KeyIdentifier keyId,
      byte[] wrappedAndPackedContentKey) throws SymphonyInputException, SymphonyEncryptionException
  {

    //
    // For historical rotationId = 0 and wrapped content key length = 48, use legacy content key un-packing mechanism.
    //
    if(wrappedAndPackedContentKey.length == 48) {
      TransportableWrappedKey wrappedContentKeyOnTheWire = null;

      try {
        wrappedContentKeyOnTheWire = new TransportableWrappedKey(wrappedAndPackedContentKey);
      } catch (IOException e) {
        String s = "Invalid wrappedContentKey for keyId: " + keyId.toString();
        throw new IllegalArgumentException(s, e);
      }

      byte[] iv = wrappedContentKeyOnTheWire.getIv();
      byte[] wrappedContentKeyBytes = wrappedContentKeyOnTheWire.getWrappedKeyBytes();

      if (wrappedContentKeyBytes.length != 32)
        throw new IllegalArgumentException(
            "wrappedContentKey in wrappedContentKeyOnTheWire is invalid, expected 32 bytes, found "
                + wrappedContentKeyBytes.length);


      byte[] accountKey = accountKeyCache_.getAccountKey(CERT_ID,
          ACCOUNT_KEY_ROTIOTON_ID_THAT_DECRYPTS_ROTATION_0_CONTENT_KEY, userId_);

      byte[] clearContentKey = AES.DecryptCBC(wrappedContentKeyBytes, accountKey, iv);
      if (clearContentKey == null || clearContentKey.length != 32) {
        throw new IllegalArgumentException(
            "CK is invalid, expected 32 bytes, found " + clearContentKey.length);
      }

      return clearContentKey;
    }

    //
    // For all content keys with rotationId >= 1
    //
    ICiphertextTransport transport = null;
    try {
      transport = CiphertextFactory.getTransport(wrappedAndPackedContentKey);
    } catch (InvalidDataException | CiphertextTransportVersionException | CiphertextTransportIsEmptyException e) {
      throw new IllegalArgumentException("Unable to get content key. Error occured in ciphertext transport. KeyIdentifier: " + keyId.toString(), e);
    }

    //
    // Fetch the account key of the appropriate accountKeyRotationId to decrypt the wrapped content key
    //
    
    RotationId rotationId = RotationId.newBuilder().build(transport.getRotationId());
    
    byte[] accountKey = accountKeyCache_.getAccountKey(CERT_ID, rotationId, userId_);


    //
    // Depending on mode, decrypt the wrapped content key.
    //
    byte[] clearContentKey = null;
    switch (transport.getEncryptionMode()) {
      case 0:
        clearContentKey = AES.DecryptGCM(transport.getCiphertext(), transport.getAuthData(), accountKey, transport.getIV(), transport.getTag());
        break;
      case 1:
        clearContentKey = AES.DecryptCBC(transport.getCiphertext(), accountKey, transport.getIV());
        break;
      default:
        StringBuilder s = new StringBuilder("Unknown encryption mode (");
        s.append((int)transport.getEncryptionMode());
        s.append(") encountered in ciphertext. Key Identifier: ").append(keyId.toString());
        throw new IllegalArgumentException(s.toString());
    }

    if (clearContentKey.length != 32) {
      throw new IllegalArgumentException(
          "CK is invalid, expected 32 bytes, found " + clearContentKey.length);
    }

    return clearContentKey;
  }
}
