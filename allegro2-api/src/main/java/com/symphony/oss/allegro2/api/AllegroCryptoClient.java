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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.impl.client.CloseableHttpClient;

import com.symphony.oss.commons.fault.CodingFault;
import com.symphony.oss.commons.immutable.ImmutableByteArray;
import com.symphony.oss.models.core.canon.facade.IEncryptedApplicationRecord;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.crypto.cipher.CipherSuite;
import com.symphony.oss.models.crypto.cipher.ICipherSuite;
import com.symphony.oss.models.internal.km.canon.KmInternalHttpModelClient;
import com.symphony.oss.models.internal.km.canon.facade.IUserKeys;
import com.symphony.oss.models.internal.pod.canon.IPodInfo;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;
import com.symphony.oss.models.internal.pod.canon.facade.IAccountInfo;
import com.symphony.security.clientsdk.entity.EntityCryptoHandler;
import com.symphony.security.clientsdk.entity.EntityCryptoHandlerV2;
import com.symphony.security.clientsdk.search.ClientTokenizer_v2;
import com.symphony.security.clientsdk.search.IClientTokenizer;
import com.symphony.security.clientsdk.transport.CiphertextFactory;
import com.symphony.security.exceptions.CiphertextTransportIsEmptyException;
import com.symphony.security.exceptions.CiphertextTransportVersionException;
import com.symphony.security.exceptions.InvalidDataException;
import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;
import com.symphony.security.helper.ClientCryptoHandler;
/**
 * Cryptographic functions for AllegroApi
 * 
 * @author Bruce Skingle
 *
 */
class AllegroCryptoClient
{
  public static final CipherSuiteId       ThreadSeurityContextCipherSuiteId = CipherSuiteId.RSA2048_AES256;

  private final CloseableHttpClient        podHttpClient_;
  private final CloseableHttpClient        kmHttpClient_;
  private final PodInternalHttpModelClient podInternalApiClient_;
  private final KmInternalHttpModelClient  kmInternalClient_;
  private final IPodInfo                   podInfo_;

  private final PodAndUserId               internalUserId_;
  private final Supplier<IAccountInfo>     accountInfoProvider_;

  private final ClientCryptoHandler        clientCryptoHandler_;
  private final AccountKeyCache            accountKeyCache_;
  private final ContentKeyCache            contentKeyCache_;
  private final ThreadRotationIdCache      threadRotationIdCache_;
  private final EntityKeyCache             entityKeyCache_;
  private final ICipherSuite               cipherSuite_;


  
  AllegroCryptoClient(CloseableHttpClient podHttpClient, PodInternalHttpModelClient podInternalApiClient,
      CloseableHttpClient kmHttpClient, KmInternalHttpModelClient kmInternalClient,
      IPodInfo podInfo, PodAndUserId internalUserId,
      Supplier<IAccountInfo> accountInfoProvider)
  {
    podHttpClient_ = podHttpClient;
    podInternalApiClient_ = podInternalApiClient;
    kmHttpClient_ = kmHttpClient;
    kmInternalClient_ = kmInternalClient;
    podInfo_ = podInfo;
    internalUserId_ = internalUserId;
    accountInfoProvider_ = accountInfoProvider;
    
    clientCryptoHandler_ = new ClientCryptoHandler();
    cipherSuite_ = CipherSuite.get(ThreadSeurityContextCipherSuiteId);
    
    IUserKeys userKeys = kmInternalClient_.newKeysMeGetHttpRequestBuilder()
        .build()
        .execute(kmHttpClient_);
    
    accountKeyCache_ = new AccountKeyCache(podHttpClient_, podInternalApiClient_, userKeys);
    contentKeyCache_ = new ContentKeyCache(podHttpClient_, podInternalApiClient_, accountKeyCache_, internalUserId);
    threadRotationIdCache_ = new ThreadRotationIdCache(podHttpClient_, podInternalApiClient_);
    entityKeyCache_ = new EntityKeyCache(kmHttpClient_, kmInternalClient_, accountKeyCache_, internalUserId, clientCryptoHandler_);
  }

  RotationId getRotationForThread(ThreadId threadId)
  {
    return threadRotationIdCache_.getRotationId(threadId);
  }

  String encryptTagV1(String plaintext)
  {
    try
    {
      return EntityCryptoHandler.encrypt(plaintext, Base64.decodeBase64(entityKeyCache_.getEntityKey().getEncodedKey().getValue()));
    }
    catch (SymphonyEncryptionException | SymphonyInputException | NoSuchAlgorithmException e)
    {
      throw new IllegalStateException(e);
    }
  }

  String encryptTagV2(String plaintext)
  {
    try
    {
      return EntityCryptoHandlerV2.encrypt(plaintext, Base64.decodeBase64(entityKeyCache_.getEntityKey().getEncodedKey().getValue()));
    }
    catch (SymphonyEncryptionException | SymphonyInputException | InvalidDataException e)
    {
      throw new IllegalStateException(e);
    }
  }

  List<String> tokenize(ThreadId threadId, String clear, Set<String> clearTokens)
  {
    RotationId rotationId = getRotationForThread(threadId);
    
    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(threadId, rotationId, internalUserId_);
    IClientTokenizer tokenizer = new ClientTokenizer_v2();
    
    ThreadId wallThreadId = accountInfoProvider_.get().getMyCurrentThreadId();
    
    AllegroCryptoHelper wallKey = contentKeyCache_.getContentKey(wallThreadId, getRotationForThread(wallThreadId), internalUserId_);
    
    try
    {
      List<String> tokenList = tokenizer.tokenize(clear, clearTokens, Base64.decodeBase64(helper.getEncodedKey().getValue()),
          Base64.decodeBase64(wallKey.getEncodedKey().getValue()), rotationId.getValue());
      
      return tokenList;
    }
    catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException e)
    {
      throw new CodingFault(e);
    }
  }

  /**
   * Encrypt the given clear text with the content key for the given thread.
   * 
   * @param threadId The id of the thread.
   * @param clearText Text to be encrypted.
   * 
   * @return cipher text.
   */
  String encrypt(ThreadId threadId, String clearText)
  {
    RotationId rotationId = getRotationForThread(threadId);
    
    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(threadId, rotationId, internalUserId_);

    return helper.encrypt(clearText, podInfo_.getPodId(), rotationId.getValue());
  }
  
  void encrypt(EncryptablePayloadBuilder<?,?> builder)
  {
    RotationId rotationId = getRotationForThread(builder.getThreadId());
    
    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(builder.getThreadId(), rotationId, internalUserId_);
    
    builder.withEncryptedPayload(cipherSuite_.encrypt(helper.getSecretKey(), builder.getPayload()))
      .withRotationId(rotationId)
      .withCipherSuiteId(cipherSuite_.getId());
  }
  
//  IApplicationObjectPayload decrypt(IStoredApp encryptedApplicationRecord)
//  {
//    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(encryptedApplicationRecord.getThreadId(), encryptedApplicationRecord.getRotationId(), internalUserId_);
//
//    ImmutableByteArray plainText = cipherSuite_.decrypt(helper.getSecretKey(), encryptedApplicationRecord.getEncryptedPayload());
//    
//    ModelRegistry objectModelRegistry = encryptedApplicationRecord instanceof IStoredApplicationObject 
//        ? new ObjectModelRegistry(modelRegistry_, (IStoredApplicationObject)encryptedApplicationRecord) 
//            : modelRegistry_;
//    
//    IEntity entity = objectModelRegistry.parseOne(plainText.getReader());
//    ApplicationObjectPayload payload;
//    
//    if(entity instanceof ApplicationObjectPayload)
//    {
//      payload = (ApplicationObjectPayload)entity;
//
//    }
//    else
//    {
//      payload = new ApplicationObjectPayload(entity.getJsonObject(), objectModelRegistry);
//    }
//    
//    return payload;
//  }
  
  ImmutableByteArray decrypt(IEncryptedApplicationRecord encryptedApplicationRecord)
  {
    return decrypt(encryptedApplicationRecord.getThreadId(), encryptedApplicationRecord.getRotationId(), encryptedApplicationRecord.getEncryptedPayload());
  }
  
  ImmutableByteArray decrypt(ThreadId threadId, RotationId rotationId, EncryptedData encryptedPayload)
  {
    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(threadId, rotationId, internalUserId_);

    return cipherSuite_.decrypt(helper.getSecretKey(), encryptedPayload);
  }

  String decrypt(ThreadId threadId, String cipherText)
  {
    RotationId  rotationId  = getRotationIdForCipherText(Base64.decodeBase64(cipherText));
    
    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(threadId, rotationId, internalUserId_);

    String plainText = helper.decrypt(cipherText);
      
    return plainText;
  }

  private RotationId getRotationIdForCipherText(byte[] cipherText)
  {
    try
    {
      return RotationId.newBuilder().build(CiphertextFactory.getTransport(cipherText).getRotationId());
    }
    catch(InvalidDataException | CiphertextTransportVersionException | CiphertextTransportIsEmptyException e)
    {
      throw new IllegalArgumentException("Unable to fetch rotation ID from cipher text", e);
    }
  }
}
