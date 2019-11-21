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

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.impl.client.CloseableHttpClient;
import org.symphonyoss.s2.canon.runtime.IEntity;
import org.symphonyoss.s2.canon.runtime.exception.NotFoundException;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.hash.Hash;

import com.symphony.oss.models.chat.canon.ChatHttpModelClient;
import com.symphony.oss.models.chat.canon.EstablishSecurityContextRequest;
import com.symphony.oss.models.chat.canon.IEstablishSecurityContextRequest;
import com.symphony.oss.models.chat.canon.facade.ThreadId;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.crypto.canon.Base64SecretKey;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.facade.WrappedKey;
import com.symphony.oss.models.crypto.cipher.CipherSuite;
import com.symphony.oss.models.crypto.cipher.ICipherSuite;
import com.symphony.oss.models.fundamental.FundamentalModelRegistry;
import com.symphony.oss.models.fundamental.canon.facade.IFundamentalObject;
import com.symphony.oss.models.fundamental.canon.facade.IOpenPrincipalCredential;
import com.symphony.oss.models.fundamental.canon.facade.IOpenSigningKey;
import com.symphony.oss.models.fundamental.canon.facade.IOpenSimpleSecurityContext;
import com.symphony.oss.models.fundamental.canon.facade.ISimpleSecurityContext;
import com.symphony.oss.models.fundamental.canon.facade.RotationId;
import com.symphony.oss.models.fundamental.canon.facade.SecurityContextMember;
import com.symphony.oss.models.fundmental.canon.FundamentalHttpModelClient;
import com.symphony.oss.models.fundmental.canon.IMemberIdObject;
import com.symphony.oss.models.fundmental.canon.ISecurityContextMemberKeys;
import com.symphony.oss.models.internal.km.canon.KmInternalHttpModelClient;
import com.symphony.oss.models.internal.km.canon.facade.IUserKeys;
import com.symphony.oss.models.internal.pod.canon.IPodInfo;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;
import com.symphony.oss.models.internal.pod.canon.facade.IAccountInfo;
import com.symphony.oss.models.system.canon.SystemHttpModelClient;
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
import com.symphony.security.helper.KeyIdentifier;
/**
 * Implementation of IAllegroCryptoClient.
 * 
 * @author Bruce Skingle
 *
 */
class AllegroCryptoClient implements IAllegroCryptoClient
{
  public static final CipherSuiteId       ThreadSeurityContextCipherSuiteId = CipherSuiteId.RSA2048_AES256;

  private final CloseableHttpClient        httpclient_;
  private final PodInternalHttpModelClient podInternalApiClient_;
  private final KmInternalHttpModelClient  kmInternalClient_;
  private final FundamentalHttpModelClient fundamentalApiClient_;
  private final ChatHttpModelClient        chatApiClient_;
  private final IPodInfo                   podInfo_;

  private final PodAndUserId               internalUserId_;
  private final Hash                       principalHash_;
  private final Supplier<IAccountInfo>     accountInfoProvider_;

  private final FundamentalModelRegistry   modelRegistry_;
  private final ClientCryptoHandler        clientCryptoHandler_;
  private final AccountKeyCache            accountKeyCache_;
  private final ContentKeyCache            contentKeyCache_;
  private final ThreadRotationIdCache      threadRotationIdCache_;
  private final EntityKeyCache             entityKeyCache_;
  private final ICipherSuite               cipherSuite_;

  private final PrincipalSupplier principalSupplier_;


  
  AllegroCryptoClient(CloseableHttpClient httpclient, PodInternalHttpModelClient podInternalApiClient,
      KmInternalHttpModelClient kmInternalClient, FundamentalHttpModelClient fundamentalApiClient,
      SystemHttpModelClient systemApiClient, ChatHttpModelClient chatApiClient, IPodInfo podInfo, PodAndUserId internalUserId,
      Hash principalHash, Supplier<IAccountInfo> accountInfoProvider, FundamentalModelRegistry modelRegistry)
  {
    httpclient_ = httpclient;
    podInternalApiClient_ = podInternalApiClient;
    kmInternalClient_ = kmInternalClient;
    fundamentalApiClient_ = fundamentalApiClient;
    chatApiClient_ = chatApiClient;
    podInfo_ = podInfo;
    internalUserId_ = internalUserId;
    principalHash_ = principalHash;
    accountInfoProvider_ = accountInfoProvider;
    
    modelRegistry_ = modelRegistry;
    
    clientCryptoHandler_ = new ClientCryptoHandler();
    cipherSuite_ = CipherSuite.get(ThreadSeurityContextCipherSuiteId);
    
    IUserKeys userKeys = kmInternalClient_.newKeysMeGetHttpRequestBuilder()
        .build()
        .execute(httpclient_);
    
    accountKeyCache_ = new AccountKeyCache(httpclient_, podInternalApiClient_, userKeys);
    contentKeyCache_ = new ContentKeyCache(httpclient_, podInternalApiClient_, accountKeyCache_, internalUserId);
    threadRotationIdCache_ = new ThreadRotationIdCache(httpclient_, podInternalApiClient_);
    entityKeyCache_ = new EntityKeyCache(httpclient_, kmInternalClient_, accountKeyCache_, internalUserId, clientCryptoHandler_);
    
    principalSupplier_ = new PrincipalSupplier(modelRegistry, systemApiClient, httpclient, userKeys);
    
    
  }
  
  private IEntity open(IFundamentalObject object)
  {
    return modelRegistry_.open(object, (IOpenPrincipalCredential)null);
  }

  @Override
  public IOpenSigningKey getSigningKey()
  {
    return principalSupplier_.getSigningKey();
  }

  @Override
  public IOpenSimpleSecurityContext getSecurityContext(Hash securityContextHash, @Nullable ThreadId threadId)
  {
    IFundamentalObject securityContextObject = fundamentalApiClient_.newObjectsObjectHashGetHttpRequestBuilder()
      .withObjectHash(securityContextHash)
      .withCurrentVersion(threadId != null)
      .build()
      .execute(httpclient_);
    
    IEntity entity = open(securityContextObject);
    
    if(entity instanceof ISimpleSecurityContext)
    {
      ISimpleSecurityContext securityContext = (ISimpleSecurityContext)entity;
      
      try
      {
        IMemberIdObject keysId = SecurityContextMember.getMemberKeysIdFor(securityContext.getAbsoluteHash(), principalHash_);
  
        IFundamentalObject keysObject = fundamentalApiClient_.newObjectsIdIdObjectHashGetHttpRequestBuilder()
          .withIdObjectHash(keysId.getAbsoluteHash())
          .build()
          .execute(httpclient_);
        
        IEntity keysEntity = open(keysObject);
        
        if(keysEntity instanceof ISecurityContextMemberKeys)
        {
          ISecurityContextMemberKeys keys = (ISecurityContextMemberKeys)keysEntity;
          
          SecretKey secretKey;
          
          if(threadId==null || keys.getEncryptedKey() == null)
          {
            if(keys.getWrappedKey() == null)
              throw new IllegalStateException("SecurityContextMemberKeys is empty! " + keys);
            
            secretKey = securityContext.getCipherSuite().unwrap(keys.getWrappedKey(), principalSupplier_.getExchangeKey().getPrivateKey());
          }
          else
          {
            RotationId rotationId = getRotationForThread(threadId);
            
            KeyIdentifier keyId = new KeyIdentifier(threadId.getValue().toByteArray(), internalUserId_.getValue(), rotationId.getValue(), null);
            byte[] contentKey;
            
            try
            {
              contentKey = contentKeyCache_.unwrapContentKey(keyId, keys.getEncryptedKey().getValue().toByteArray());
            }
            catch (SymphonyInputException | SymphonyEncryptionException e)
            {
              throw new IllegalStateException("Unable to recover security context keys.", e);
            }
            
            Base64SecretKey encodedKey = Base64SecretKey.newBuilder().build(Base64.encodeBase64String(contentKey));
            
            secretKey          = cipherSuite_.secretKeyFromBase64(encodedKey);
          }
          //SecretKey secretKey2 = securityContext.getCipherSuite().unwrap(keys.getWrappedKey(), principalSupplier_.getExchangeKey().getPrivateKey());

          return securityContext.open(secretKey);
        }
      }
      catch(NotFoundException e)
      {
        // No wrapped keys, see if we can construct them from the keymanager....
        if(threadId == null)
        {
          throw e;
        }
        else
        {
          RotationId rotationId = getRotationForThread(threadId);
          
          AllegroCryptoHelper helper = contentKeyCache_.getContentKey(threadId, rotationId, internalUserId_);
  
          SecretKey secretKey = helper.getSecretKey();
          
          return securityContext.open(secretKey);
        }
      }
    }
    
    throw new IllegalStateException("Not a security context.");
  }

  @Override
  public IOpenSimpleSecurityContext getOrCreateThreadSecurityContext(ThreadId threadId)
  {
    RotationId rotationId = getRotationForThread(threadId);
    
    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(threadId, rotationId, internalUserId_);

    WrappedKey wrappedKey = cipherSuite_.wrap(helper.getSecretKey(), principalSupplier_.getExchangeKey().getPublicKey());
    
    IEstablishSecurityContextRequest request = new EstablishSecurityContextRequest.Builder()
      .withThreadId(threadId)
      .withRotationId(rotationId)
      .withExchangeKeyHash(principalSupplier_.getExchangeKey().getAbsoluteHash())
      .withCipherSuiteId(cipherSuite_.getId())
      .withWrappedKey(wrappedKey)
      .withEncryptedKey(helper.getEncryptedKey())
      .build();
    
    IFundamentalObject response = chatApiClient_.newSecurityContextsEstsablishPostHttpRequestBuilder()
      .withCanonPayload(request)
      .build()
      .execute(httpclient_);
    
    IEntity responsePayload = open(response);
    
    if(!(responsePayload instanceof ISimpleSecurityContext))
      throw new IllegalStateException("Retrieved security context is a " + responsePayload.getClass().getName() + ", not IVersionedSecurityContext");
    
    ISimpleSecurityContext securityContext = (ISimpleSecurityContext)responsePayload;
    
    IOpenSimpleSecurityContext openSecurityContext = securityContext.open(helper.getSecretKey());
    
    return openSecurityContext;
  }

  @Override
  public RotationId getRotationForThread(ThreadId threadId)
  {
    return threadRotationIdCache_.getRotationId(threadId);
  }

  @Override
  public String encryptTagV1(String plaintext)
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

  @Override
  public String encryptTagV2(String plaintext)
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

  @Override
  public List<String> tokenize(ThreadId threadId, String clear, Set<String> clearTokens)
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

  @Override
  public String encrypt(ThreadId threadId, String clearText)
  {
    RotationId rotationId = getRotationForThread(threadId);
    
    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(threadId, rotationId, internalUserId_);

    return helper.encrypt(clearText, podInfo_.getPodId(), rotationId.getValue());
  }

  @Override
  public String decrypt(ThreadId threadId, String cipherText)
  {
    RotationId  rotationId  = getRotationIdForCipherText(Base64.decodeBase64(cipherText));
    
    AllegroCryptoHelper helper = contentKeyCache_.getContentKey(threadId, rotationId, internalUserId_);

    String plainText = helper.decrypt(cipherText);
      
//      EncryptedData encryptedData = EncryptedData.newBuilder().build(ImmutableByteArray.newInstance(Base64.decodeBase64(cipherText)));
//      ImmutableByteArray p2 = helper.getCiphersuite().decrypt(helper.getSecretKey(), encryptedData);
//      
//      String s2 = p2.toString();
//      
//      System.err.println(plainText);
//      System.err.println(s2);
      
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
