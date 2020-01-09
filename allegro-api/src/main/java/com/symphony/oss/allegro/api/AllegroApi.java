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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.canon.runtime.EntityBuilder;
import org.symphonyoss.s2.canon.runtime.IEntityFactory;
import org.symphonyoss.s2.canon.runtime.ModelRegistry;
import org.symphonyoss.s2.canon.runtime.http.client.IAuthenticationProvider;
import org.symphonyoss.s2.canon.runtime.jjwt.JwtBase;
import org.symphonyoss.s2.common.dom.json.IImmutableJsonDomNode;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonObject;
import org.symphonyoss.s2.common.dom.json.MutableJsonObject;
import org.symphonyoss.s2.common.dom.json.jackson.JacksonAdaptor;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fault.TransientTransactionFault;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.IFugueLifecycleComponent;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.core.trace.NoOpContextFactory;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;
import org.symphonyoss.symphony.messageml.MessageMLContext;
import org.symphonyoss.symphony.messageml.elements.Chime;
import org.symphonyoss.symphony.messageml.elements.FormatEnum;
import org.symphonyoss.symphony.messageml.elements.MessageML;
import org.symphonyoss.symphony.messageml.exceptions.InvalidInputException;
import org.symphonyoss.symphony.messageml.exceptions.ProcessingException;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Files;
import com.symphony.oss.allegro.api.agent.util.EncryptionHandler;
import com.symphony.oss.allegro.api.agent.util.V4MessageTransformer;
import com.symphony.oss.allegro.api.auth.AuthHandler;
import com.symphony.oss.allegro.api.request.FetchFeedObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchPartitionObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchRecentMessagesRequest;
import com.symphony.oss.allegro.api.request.PartitionId;
import com.symphony.oss.allegro.api.request.SubscribeFeedObjectsRequest;
import com.symphony.oss.allegro.api.request.UpsertFeedRequest;
import com.symphony.oss.allegro.api.request.UpsertPartitionRequest;
import com.symphony.oss.model.chat.LiveCurrentMessageFactory;
import com.symphony.oss.models.allegro.canon.EntityJson;
import com.symphony.oss.models.allegro.canon.IReceivedSocialMessage;
import com.symphony.oss.models.allegro.canon.ReceivedMaestroMessage;
import com.symphony.oss.models.allegro.canon.ReceivedSocialMessage;
import com.symphony.oss.models.allegro.canon.facade.ChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.allegro.canon.facade.ReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ChatHttpModelClient;
import com.symphony.oss.models.chat.canon.ChatModel;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.chat.canon.IMaestroMessage;
import com.symphony.oss.models.chat.canon.facade.ISocialMessage;
import com.symphony.oss.models.core.canon.CoreHttpModelClient;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.core.canon.HashType;
import com.symphony.oss.models.core.canon.ICursors;
import com.symphony.oss.models.core.canon.IPagination;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.core.canon.facade.UserId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.CryptoModel;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.crypto.canon.PemPrivateKey;
import com.symphony.oss.models.crypto.cipher.CipherSuite;
import com.symphony.oss.models.crypto.cipher.ICipherSuite;
import com.symphony.oss.models.internal.km.canon.KmInternalHttpModelClient;
import com.symphony.oss.models.internal.km.canon.KmInternalModel;
import com.symphony.oss.models.internal.pod.canon.IMessageEnvelope;
import com.symphony.oss.models.internal.pod.canon.IPodInfo;
import com.symphony.oss.models.internal.pod.canon.IThreadOfMessages;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;
import com.symphony.oss.models.internal.pod.canon.PodInternalModel;
import com.symphony.oss.models.internal.pod.canon.facade.IAccountInfo;
import com.symphony.oss.models.object.canon.DeletionType;
import com.symphony.oss.models.object.canon.FeedRequest;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IFeed;
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;
import com.symphony.oss.models.object.canon.IUserIdObject;
import com.symphony.oss.models.object.canon.IUserPermissionsRequest;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;
import com.symphony.oss.models.object.canon.ObjectModel;
import com.symphony.oss.models.object.canon.PartitionsPartitionHashPageGetHttpRequestBuilder;
import com.symphony.oss.models.object.canon.UserPermissionsRequest;
import com.symphony.oss.models.object.canon.facade.DeletedApplicationObject;
import com.symphony.oss.models.object.canon.facade.FeedObjectDelete;
import com.symphony.oss.models.object.canon.facade.FeedObjectExtend;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IFeedObject;
import com.symphony.oss.models.object.canon.facade.IFeedObjectExtend;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.SortKey;
import com.symphony.oss.models.object.canon.facade.StoredApplicationObject;
import com.symphony.oss.models.pod.canon.IPodCertificate;
import com.symphony.oss.models.pod.canon.IUserV2;
import com.symphony.oss.models.pod.canon.PodHttpModelClient;
import com.symphony.oss.models.pod.canon.PodModel;

/**
 * Implementation of IAllegroApi, the main Allegro API class.
 * 
 * @author Bruce Skingle
 *
 */
public class AllegroApi implements IAllegroApi
{

  static final int   ALLOWED_PERMISSIONS_READ         = PERMISSION_READ;
//  static final int   ALLOWED_PERMISSIONS_READ_WRITE   = PERMISSION_READ | PERMISSION_WRITE;
  
  private static final Logger                   log_                       = LoggerFactory.getLogger(AllegroApi.class);

  private static final String                   FORMAT_MESSAGEMLV2         = "com.symphony.messageml.v2";
  private static final ObjectMapper             OBJECT_MAPPER              = new ObjectMapper();  // TODO: get rid of this
  private static final int                      ENCRYPTION_ORDINAL         = 0;
  private static final int                      MEIDA_ENCRYPTION_ORDINAL   = 2;
  private static final long                     FAILED_CONSUMER_RETRY_TIME    = TimeUnit.SECONDS.toSeconds(30);
  
  private static final ObjectMapper  AUTO_CLOSE_MAPPER = new ObjectMapper().configure(Feature.AUTO_CLOSE_SOURCE, false);
  
  private final String                          userName_;
  private final ModelRegistry                   modelRegistry_;
  private final String                          clientType_;
  private final ICipherSuite                    cipherSuite_;
  private final CloseableHttpClient             httpClient_;
  private final AuthHandler                     authHandler_;
  private final PodHttpModelClient              podApiClient_;
  private final PodInternalHttpModelClient      podInternalApiClient_;
  private final KmInternalHttpModelClient       kmInternalClient_;
  private final IAllegroCryptoClient            cryptoClient_;
  private final IPodInfo                        podInfo_;
  private final AllegroDataProvider             dataProvider_;
  private final V4MessageTransformer            messageTramnsformer_;
  private final EncryptionHandler               agentEncryptionHandler_;
  private final CoreHttpModelClient             coreApiClient_;
  private final ChatHttpModelClient             chatApiClient_;
  private final ObjectHttpModelClient           objectApiClient_;
  private final PodAndUserId                    userId_;
  private final PemPrivateKey                   rsaPemCredential_;

  private PodAndUserId                          internalUserId_;
  private PodId                                 podId_;

  private final Supplier<IAccountInfo>          accountInfoProvider_;
  private final Supplier<X509Certificate>       podCertProvider_;

  private final ITraceContextTransactionFactory traceContextFactory_;

  private LiveCurrentMessageFactory             liveCurrentMessageFactory_ = new LiveCurrentMessageFactory();
  
  /**
   * Constructor.
   * 
   * @param builder The builder containing all initialisation values.
   */
  public AllegroApi(AbstractBuilder<?,?> builder)
  {
    log_.info("AllegroApi constructor start");
    
    traceContextFactory_ = new NoOpContextFactory();
    
    userName_ = builder.userName_;
    
    modelRegistry_ = new ModelRegistry()
        .withFactories(ObjectModel.FACTORIES)
        .withFactories(CryptoModel.FACTORIES)
        .withFactories(CoreModel.FACTORIES)
        .withFactories(ChatModel.FACTORIES)
        .withFactories(PodModel.FACTORIES)
        .withFactories(PodInternalModel.FACTORIES)
        .withFactories(KmInternalModel.FACTORIES)
        ;
    
    for(IEntityFactory<?, ?, ?> factory : builder.factories_)
      modelRegistry_.withFactories(factory);
    
    clientType_     = getClientVersion();
    cipherSuite_    = builder.cipherSuite_;
    httpClient_     = builder.httpclient_;
    

    podApiClient_ = new PodHttpModelClient(
        modelRegistry_,
        builder.podUrl_, "/pod", null);
    
    authHandler_    = new AuthHandler(httpClient_, builder.cookieStore_,
        builder.podUrl_, builder.rsaCredential_, userName_);
    
    log_.info("sbe auth....");
    authHandler_.authenticate(true, false);
    
    log_.info("fetch podInfo_....");
    podInternalApiClient_ = new PodInternalHttpModelClient(
        modelRegistry_,
        builder.podUrl_, null, null);
    
    accountInfoProvider_ = new Supplier<IAccountInfo>()
    {
      private IAccountInfo value_;

      @Override
      public synchronized IAccountInfo get()
      {
        if(value_ == null)
        {
          value_ = podInternalApiClient_.newWebcontrollerMaestroAccountGetHttpRequestBuilder()
              .withClienttype(clientType_)
              .build()
              .execute(httpClient_);
        }
        return value_;
      }
    };
    
    podCertProvider_ =  new Supplier<X509Certificate>()
    {
      private X509Certificate            value_;

      @Override
      public synchronized X509Certificate get()
      {
        if(value_ == null)
        {
          log_.info("fetch podCert....");
          IPodCertificate podCert = podApiClient_.newV1PodcertGetHttpRequestBuilder()
              .build()
              .execute(httpClient_);
            
          log_.info("fetch podCert....got " + podCert.getCertificate());
          value_ = cipherSuite_.certificateFromPem(podCert.getCertificate());
        }
        return value_;
      }
    };

    podInfo_ = getPodInfo();
    log_.info("fetch podInfo_...." + podInfo_);
    
    log_.info("keymanager auth....");
    podId_ = PodId.newBuilder().build(podInfo_.getExternalPodId());
    authHandler_.setKeyManagerUrl(podInfo_.getKeyManagerUrl());
    authHandler_.authenticate(false, true);
    log_.info("keymanager auth...." + authHandler_.getKeyManagerToken());
    
    
    if(authHandler_.getUserId() == null)
    {
      log_.info("getAccountInfo....");
      IAccountInfo accountInfo = accountInfoProvider_.get();
      log_.info("getAccountInfo...." + accountInfo);
      
      internalUserId_ = PodAndUserId.newBuilder().build(accountInfo.getUserName());
      userId_ = toExternalUserId(internalUserId_);
    }
    else
    {
      internalUserId_ = userId_ = authHandler_.getUserId();
    }
    
    kmInternalClient_ = new KmInternalHttpModelClient(
        modelRegistry_,
        podInfo_.getKeyManagerUrl(), null, null);
    
    dataProvider_ = new AllegroDataProvider(httpClient_, podApiClient_, podInfo_, authHandler_.getSessionToken());
    
    IAuthenticationProvider jwtGenerator  = new IAuthenticationProvider()
    {
      @Override
      public void authenticate(RequestBuilder builder)
      {
        builder.addHeader(JwtBase.AUTH_HEADER_KEY, JwtBase.AUTH_HEADER_VALUE_PREFIX + authHandler_.getSessionToken());
      }
    };
    
    coreApiClient_  = new CoreHttpModelClient(
        modelRegistry_,
        builder.objectStoreUrl_, null, jwtGenerator);

    objectApiClient_  = new ObjectHttpModelClient(
        modelRegistry_,
        builder.objectStoreUrl_, null, jwtGenerator);
    
    chatApiClient_  = new ChatHttpModelClient(
        modelRegistry_,
        builder.objectStoreUrl_, null, jwtGenerator);
    
    cryptoClient_ = new AllegroCryptoClient(httpClient_, podInternalApiClient_, kmInternalClient_, coreApiClient_,
        chatApiClient_,
        podInfo_, internalUserId_,
        accountInfoProvider_,
        modelRegistry_);
    
    messageTramnsformer_= new V4MessageTransformer(clientType_);
    
    agentEncryptionHandler_ = new EncryptionHandler(cryptoClient_);
    
    rsaPemCredential_ = builder.rsaPemCredential_;
    
    log_.info("userId_ = " + userId_);
    
    log_.info("allegroApi constructor done.");
  }
  
  /**
   * Convert the given internal or external user ID into an external user ID.
   * 
   * @param internalOrExternalUserId  An internal or external user ID
   * 
   * @return The external user ID for the given user ID.
   */
  public PodAndUserId toExternalUserId(PodAndUserId internalOrExternalUserId)
  {
    return PodAndUserId.newBuilder().build(UserId.replacePodId(internalOrExternalUserId.longValue(), podInfo_.getExternalPodId()));
  }

  private String getClientVersion()
  {
    String osName = System.getProperty("os.name");
    String osVersion = System.getProperty("os.version");
    String chatAppName = "allegro";
    String chatAppVersion = getVersion();
    return String.format("%s-%s-%s-%s", chatAppName, chatAppVersion, osName, osVersion);
  }
  
  @Override
  public String getKeyManagerToken()
  {
    return authHandler_.getKeyManagerToken();
  }

  @Override
  public String getSessionToken()
  {
    return authHandler_.getSessionToken();
  }

  @Override
  public PodId getPodId()
  {
    return podId_;
  }
  
//  @Override
//  public IFundamentalObject fetchAbsolute(Hash absoluteHash)
//  {
//    return fundamentalApiClient_.newObjectsObjectHashGetHttpRequestBuilder()
//        .withCurrentVersion(false)
//        .withObjectHash(absoluteHash)
//        .build()
//        .execute(httpClient_);
//  }
//
//  @Override
//  public <T extends IEntity> T fetchAbsolute(Hash absoluteHash, Class<T> type)
//  {
//    IEntity openEntity = open(fetchAbsolute(absoluteHash));
//    
//    if(type.isInstance(openEntity))
//      return type.cast(openEntity);
//    
//    throw new IllegalStateException("Received object is of type " + openEntity.getCanonType());
//  }
//
//  @Override
//  public <T extends IEntity> T fetchCurrent(Hash baseHash, Class<T> type)
//  {
//    IEntity openEntity = open(fetchCurrent(baseHash));
//    
//    if(type.isInstance(openEntity))
//      return type.cast(openEntity);
//    
//    throw new IllegalStateException("Received object is of type " + openEntity.getCanonType());
//  }
//
//  @Override
//  public IFundamentalObject fetchCurrent(Hash baseHash)
//  {
//    return fundamentalApiClient_.newObjectsObjectHashGetHttpRequestBuilder()
//        .withCurrentVersion(true)
//        .withObjectHash(baseHash)
//        .build()
//        .execute(httpClient_);
//  }
//  
  @Override
  public void store(IAbstractStoredApplicationObject object)
  {
    objectApiClient_.newObjectsObjectHashPutHttpRequestBuilder()
      .withObjectHash(object.getAbsoluteHash())
      .withCanonPayload(object)
      .build()
      .execute(httpClient_);
  }
  
  @Override
  public IFugueLifecycleComponent subscribeToFeed(SubscribeFeedObjectsRequest request)
  {
    IThreadSafeErrorConsumer<IAbstractStoredApplicationObject> unprocessableConsumer = new IThreadSafeErrorConsumer<IAbstractStoredApplicationObject>()
    {
      @Override
      public void consume(IAbstractStoredApplicationObject item, ITraceContext trace, String message, Throwable cause)
      {
        request.getConsumerManager().getUnprocessableMessageConsumer().consume(item, trace, message, cause);
      }

      @Override
      public void close()
      {
        request.getConsumerManager().getUnprocessableMessageConsumer().close();
      }
    };
    
    AllegroSubscriberManager subscriberManager = new AllegroSubscriberManager.Builder()
        .withHttpClient(httpClient_)
        .withObjectApiClient(objectApiClient_)
        .withTraceContextTransactionFactory(traceContextFactory_)
        .withUnprocessableMessageConsumer(unprocessableConsumer)
        .withSubscription(new AllegroSubscription(request, this))
        .withSubscriberThreadPoolSize(request.getSubscriberThreadPoolSize())
        .withHandlerThreadPoolSize(request.getHandlerThreadPoolSize())
      .build();
    
    return subscriberManager;
  }
  
  @Override
  public void fetchFeedObjects(FetchFeedObjectsRequest request)
  {
    Hash feedHash = request.getHash(getUserId());
    
    try(ITraceContextTransaction traceTransaction = traceContextFactory_.createTransaction("FetchFeed", feedHash.toString()))
    {
      ITraceContext trace = traceTransaction.open();
      
      List<IFeedObject> messages  = objectApiClient_.newFeedsFeedHashObjectsPostHttpRequestBuilder()
          .withFeedHash(feedHash)
          .withCanonPayload(new FeedRequest.Builder()
              .withMaxItems(request.getMaxItems() != null ? request.getMaxItems() : 1)
              .build())
          .build()
          .execute(httpClient_);
      
      FeedRequest.Builder builder = new FeedRequest.Builder()
          .withMaxItems(0)
          .withWaitTimeSeconds(0);
      int ackCnt = 0;
      
      for(IFeedObject message : messages)
      {
        try
        {
          request.getConsumerManager().consume(message.getPayload(), trace, this);
            
          builder.withDelete(new FeedObjectDelete.Builder()
              .withReceiptHandle(message.getReceiptHandle())
              .build()
              );
          ackCnt++;
        }
        catch (TransientTransactionFault e)
        {
          log_.warn("Transient processing failure, will retry (forever)", e);
          builder.withExtend(createExtend(message.getReceiptHandle(), e.getRetryTime(), e.getRetryTimeUnit()));
          ackCnt++;
        }
        catch(RetryableConsumerException e)
        {
          log_.warn("Transient processing failure, will retry (forever)", e);
          builder.withExtend(createExtend(message.getReceiptHandle(), e.getRetryTime(), e.getRetryTimeUnit()));
          ackCnt++;
        }
        catch (RuntimeException  e)
        {
          log_.warn("Unexpected processing failure, will retry (forever)", e);
          builder.withExtend(createExtend(message.getReceiptHandle(), null, null));
          ackCnt++;
        }
        catch (FatalConsumerException e)
        {
          log_.error("Unprocessable message, aborted", e);

          trace.trace("MESSAGE_IS_UNPROCESSABLE");
          
          builder.withDelete(new FeedObjectDelete.Builder()
              .withReceiptHandle(message.getReceiptHandle())
              .build()
              );
          ackCnt++;
          
          request.getConsumerManager().consumeUnprocessable(message.getPayload(), trace, "Unprocessable message, aborted", e);
        }
      }
      
      if(ackCnt>0)
      {
        // Delete (ACK) the consumed messages
        messages = objectApiClient_.newFeedsFeedHashObjectsPostHttpRequestBuilder()
            .withFeedHash(feedHash)
            .withCanonPayload(builder.build())
            .build()
            .execute(httpClient_);
      }
    }
    
    request.getConsumerManager().closeConsumers();
  }

  private IFeedObjectExtend createExtend(String receiptHandle, Long retryTime, TimeUnit timeUnit)
  {
    long delay = retryTime == null || timeUnit == null ? FAILED_CONSUMER_RETRY_TIME : timeUnit.toSeconds(retryTime);
    
    return new FeedObjectExtend.Builder()
        .withReceiptHandle(receiptHandle)
        .withVisibilityTimeout((int)delay)
        .build();
  }

  @Override
  public IFeed upsertFeed(UpsertFeedRequest request)
  {
    //ObjectModelUtils.validateFeedName(request.getName());
    

    List<IUserPermissionsRequest> userPermissions = new LinkedList<>();
    
    if(request.getPermissions() != null)
    {
      for(Entry<PodAndUserId, Set<Permission>> userPermission : request.getPermissions().getUserPermissions().entrySet())
      {
        userPermissions.add(new UserPermissionsRequest.Builder()
          .withUserId(userPermission.getKey())
          .withPermissions(toCanonPermissions(userPermission.getValue()))
          .build()
          );
      }
    }
    
    return objectApiClient_.newFeedsUpsertPostHttpRequestBuilder()
      .withCanonPayload(new com.symphony.oss.models.object.canon.UpsertFeedRequest.Builder()
        .withFeedId(request.getAndValidateId(getUserId()))
        .withPartitionHashes(request.getPartitionHashes(getUserId()))
        .withUserPermissions(userPermissions)
        .build())
      .build()
      .execute(httpClient_)
      ;
  }
  
  private Set<com.symphony.oss.models.object.canon.Permission> toCanonPermissions(Set<Permission> permissions)
  {
    Set<com.symphony.oss.models.object.canon.Permission> canonPermissions = new HashSet<>();
    
    for(Permission p : permissions)
    {
      switch(p)
      {
        case Read:
          canonPermissions.add(com.symphony.oss.models.object.canon.Permission.READ);
          break;
          
        case Write:
          canonPermissions.add(com.symphony.oss.models.object.canon.Permission.WRITE);
          break;
          
        default:
          throw new IllegalArgumentException("Unknown permission " + p);
      }
    }
    
    return canonPermissions;
  }

  @Override
  public IPartition upsertPartition(UpsertPartitionRequest request)
  {
    List<IUserPermissionsRequest> userPermissions = new LinkedList<>();
    
    if(request.getPermissions() != null)
    {
      for(Entry<PodAndUserId, Set<Permission>> userPermission : request.getPermissions().getUserPermissions().entrySet())
      {
        userPermissions.add(new UserPermissionsRequest.Builder()
          .withUserId(userPermission.getKey())
          .withPermissions(toCanonPermissions(userPermission.getValue()))
          .build()
          );
      }
    }
    
    return objectApiClient_.newPartitionsUpsertPostHttpRequestBuilder()
      .withCanonPayload(new com.symphony.oss.models.object.canon.UpsertPartitionRequest.Builder()
        .withPartitionId(request.getAndValidateId(getUserId()))
        .withUserPermissions(userPermissions)
        .build())
      .build()
      .execute(httpClient_)
      ;
  }

  private String getVersion()
  {
    String version = getClass().getPackage().getImplementationVersion();
    if (StringUtils.isEmpty(version) || "null".equals(version)) {
      version = "Unknown";
    }
    return version;
  }

  @Override
  public X509Certificate getPodCert()
  {
    return podCertProvider_.get();
  }

  @Override
  public PodAndUserId getUserId()
  {
    return userId_;
  }

  @Override
  public void authenticate()
  {
    authHandler_.authenticate(true, true);
  }
  
  @Override
  public IUserV2 getUserInfo()
  {
    return podApiClient_.newV2UserGetHttpRequestBuilder()
        .withSessionToken(authHandler_.getSessionToken())
        .withUid(userId_)
        .build()
        .execute(httpClient_);
  }
  
  
  IPodInfo getPodInfo()
  {
    return podInternalApiClient_.newWebcontrollerPublicPodInfoGetHttpRequestBuilder()
        .build()
        .execute(httpClient_)
        .getData();
  }

  @Override
  public IUserV2 getSessioninfo()
  {
    return podApiClient_.newV2SessioninfoGetHttpRequestBuilder()
        .withSessionToken(authHandler_.getSessionToken())
        .build()
        .execute(httpClient_);
  }

  @Override
  public String getMessage(String messageId)
  {
    String urlSafeMessageId = Base64.encodeBase64URLSafeString(Base64.decodeBase64(messageId));
    
    ISocialMessage encryptedMessage = podInternalApiClient_.newWebcontrollerDataqueryRetrieveMessagePayloadGetHttpRequestBuilder()
      .withMessageId(urlSafeMessageId)
      .build()
      .execute(httpClient_);
    
    return encryptedMessage.toString();
  }

  @Override
  public void fetchRecentMessagesFromPod(FetchRecentMessagesRequest request)
  {
    try(ITraceContextTransaction traceTransaction = traceContextFactory_.createTransaction("FetchRecentMessages", request.getThreadId().toBase64String()))
    {
      ITraceContext trace = traceTransaction.open();
      
      IThreadOfMessages thread = podInternalApiClient_.newDataqueryApiV3MessagesThreadGetHttpRequestBuilder()
          .withId(request.getThreadId().toBase64UrlSafeString())
          .withFrom(0L)
          .withLimit(request.getMaxItems())
          .withExcludeFields("tokenIds")
          .build()
          .execute(httpClient_);
        
      for(IMessageEnvelope envelope : thread.getEnvelopes())
      {
        ILiveCurrentMessage lcmessage = liveCurrentMessageFactory_.newLiveCurrentMessage(envelope.getMessage().getJsonObject().mutify(), modelRegistry_);
        
        try
        {
          request.getConsumerManager().consume(lcmessage, trace, this);
        }
        catch (RetryableConsumerException | FatalConsumerException e)
        {
          request.getConsumerManager().consumeUnprocessable(lcmessage, trace, "Failed to process message", e);
        }
      }
    }
  }

//  @Override
//  public void fetchRecentMessages(FetchRecentMessagesRequest request)
//  {
//    fetchMessages(request, false);
//  }
//
//  @Override
//  public void fetchMessages(FetchMessagesRequest request)
//  {
//    fetchMessages(request, request.isScanForwards());
//  }
//
//  private void fetchMessages(AbstractFetchRecentMessagesRequest<?> request, boolean scanForwards)
//  {
//    try(ITraceContextTransaction traceTransaction = traceContextFactory_.createTransaction("FetchRecentMessages", request.getThreadId().toBase64String()))
//    {
//      ITraceContext trace = traceTransaction.open();
//      
//      IThreadIdObject threadIdObject = new ThreadIdObject.Builder()
//        .withPodId(podId_)
//        .withThreadId(request.getThreadId())
//        .build();
//      
//      IFundamentalId sequence = Stream.getStreamContentSequenceId(threadIdObject);
//      
//      int maxMessages = request.getMaxMessages() == null ? 5 : request.getMaxMessages();
//      String after = null;
//      
//      do
//      {
//        IPageOfFundamentalObject page = fundamentalApiClient_.newSequencesSequenceHashPageGetHttpRequestBuilder()
//          .withSequenceHash(sequence.getAbsoluteHash())
//          .withScanForwards(scanForwards)
//          .withLimit(request.getMaxMessages())
//          .withAfter(after)
//          .build()
//          .execute(httpClient_);
//        
//        after = getAfter(page);
//        
//        for(IFundamentalObject item : page.getData())
//        {
//          request.consume(item, trace, this);
//          maxMessages--;
//        }
//      } while(after != null && maxMessages>0);
//    }
//  }
//  
//  private String getAfter(IPageOfFundamentalObject page)
//  {
//    IPagination p = page.getPagination();
//    
//    if(p == null)
//      return null;
//    
//    ICursors c = p.getCursors();
//    
//    if(c == null)
//      return null;
//    
//    return c.getAfter();
//  }
//
//  @Deprecated
//  private void handleFetchedMessage(Consumer<IChatMessage> consumer, IEntity entity)
//  {
//    switch(entity.getCanonType())
//    {
//      case SocialMessage.TYPE_ID:
//        consumer.accept(decryptChatMessage((ISocialMessage) entity));
//        break;
//        
//      case MaestroMessage.TYPE_ID:
//        consumer.accept(maestroMessage((IMaestroMessage) entity));
//        break;
//        
//      default:
//        //
//        break;
//    }
//  }
//
//
//  private IChatMessage maestroMessage(IMaestroMessage message)
//  {
//    ReceivedChatMessage.Builder builder = new ReceivedChatMessage.Builder()
//        .withMessageId(message.getMessageId())
//        .withThreadId(message.getThreadId())
//        .withPresentationML(message.getEvent() + " " + message.getVersion() + " message");
//        ;
//        
//    return builder.build();
//  }
  
  @Override
  public ApplicationObjectBuilder newApplicationObjectBuilder()
  {
    return new ApplicationObjectBuilder();
  }

  @Override
  public ApplicationObjectUpdater newApplicationObjectUpdater(IApplicationObjectPayload existingObject)
  {
    return new ApplicationObjectUpdater(existingObject);
  }
  
  @Override
  public ApplicationObjectDeleter newApplicationObjectDeleter(IApplicationObjectPayload existingObject)
  {
    return new ApplicationObjectDeleter(existingObject);
  }

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
  abstract class EncryptablePayloadbuilder<T extends EncryptablePayloadbuilder<T>> extends EntityBuilder<T, StoredApplicationObject>
  {
    protected final StoredApplicationObject.Builder  builder_ = new StoredApplicationObject.Builder();
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

    public ThreadId getThreadId()
    {
      return builder_.getThreadId();
    }

    T withEncryptedPayload(
        EncryptedData value)
    {
      builder_.withEncryptedPayload(value);
      
      return self();
    }

    T withCipherSuiteId(
        CipherSuiteId value)
    {
      builder_.withCipherSuiteId(value);
      
      return self();
    }

    T withRotationId(RotationId value)
    {
      builder_.withRotationId(value);
      
      return self();
    }
  }
  
  /**
   * Super class for AppplicationObject builder and updater.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The concrete type for fluent methods.
   */
  abstract class BaseApplicationObjectBuilder<T extends BaseApplicationObjectBuilder<T>> extends EncryptablePayloadbuilder<T>
  {
    BaseApplicationObjectBuilder(Class<T> type)
    {
      super(type);
    }
    
    BaseApplicationObjectBuilder(Class<T> type,
        IStoredApplicationObject existing)
    {
      super(type);
      
      builder_.withPartitionHash(existing.getPartitionHash())
        .withSortKey(existing.getSortKey())
        .withOwner(getUserId())
        .withPurgeDate(existing.getPurgeDate())
        .withBaseHash(existing.getBaseHash())
        .withPrevHash(existing.getAbsoluteHash())
        .withPrevSortKey(existing.getSortKey())
        ;
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

    /**
     * Set the unencrypted header for this object.
     * 
     * @param header The unencrypted header for this object.
     * 
     * @return This (fluent method).
     */
    public T withHeader(IApplicationObjectHeader header)
    {
      builder_.withHeader(header);
      
      return self();
    }

    /**
     * Set the purge date for this object.
     * 
     * @param purgeDate The date after which this object may be deleted by the system.
     * 
     * @return This (fluent method).
     */
    public T withPurgeDate(Instant purgeDate)
    {
      builder_.withPurgeDate(purgeDate);
      
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
    
    /**
     * Set the sort key for the object.
     * 
     * @param sortKey The sort key to be attached to this object within its partition.
     * 
     * @return This (fluent method).
     */
    public T withSortKey(SortKey sortKey)
    {
      builder_.withSortKey(sortKey);
      
      return self();
    }
    
    /**
     * Set the sort key for the object.
     * 
     * @param sortKey The sort key to be attached to this object within its partition.
     * 
     * @return This (fluent method).
     */
    public T withSortKey(String sortKey)
    {
      builder_.withSortKey(sortKey);
      
      return self();
    }
    
    @Override
    protected void validate()
    {
      if(builder_.getHashType() == null)
        builder_.withHashType(HashType.newBuilder().build(Hash.getDefaultHashTypeId()));
      
      if(getThreadId() == null)
        throw new IllegalStateException("ThreadId is required.");
      
      if(payload_ == null)
        throw new IllegalStateException("Payload is required.");
      
      builder_.withOwner(getUserId());
      
      cryptoClient_.encrypt(this);
      
      super.validate();
    }
  }
  
  /**
   * Builder for Application Objects.
   * 
   * @author Bruce Skingle
   *
   */
  public class ApplicationObjectBuilder extends BaseApplicationObjectBuilder<ApplicationObjectBuilder>
  {
    ApplicationObjectBuilder()
    {
      super(ApplicationObjectBuilder.class);
    }

    /**
     * Set the id of the thread with whose content key this object will be encrypted.
     * 
     * @param threadId The id of the thread with whose content key this object will be encrypted.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withThreadId(ThreadId threadId)
    {
      builder_.withThreadId(threadId);
      
      return self();
    }
    
    /**
     * Set the partition key for the object from the given partition.
     * 
     * @param partitionHash The Hash of the partition.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withPartition(Hash partitionHash)
    {
      builder_.withPartitionHash(partitionHash);
      
      return self();
    }
    
    /**
     * Set the partition key for the object from the given partition.
     * 
     * @param partitionId The ID of the partition.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withPartition(PartitionId partitionId)
    {
      builder_.withPartitionHash(partitionId.getId(getUserId()).getHash());
      
      return self();
    }
    
    /**
     * Set the partition key for the object from the given partition.
     * 
     * @param partition A partition object.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withPartition(IPartition partition)
    {
      builder_.withPartitionHash(partition.getId().getHash());
      
      return self();
    }
    
    @Override
    protected StoredApplicationObject construct()
    {
      // Nasty cast here, can't easily be avoided and it's quite safe but nonetheless.....
      return (StoredApplicationObject) builder_.build();
    }
  }
    
  @Override
  public void delete(IApplicationObjectPayload existingObject, DeletionType deletionType)
  {
    DeletedApplicationObject deletedObject = newApplicationObjectDeleter(existingObject)
      .withDeletionType(deletionType)
      .build()
      ;
    
    store(deletedObject);
  }
  
  /**
   * Builder for application type FundamentalObjects which takes an existing ApplicationObject for which a new
   * version is to be created.
   * 
   * @author Bruce Skingle
   *
   */
  public class ApplicationObjectUpdater extends BaseApplicationObjectBuilder<ApplicationObjectUpdater>
  {
    /**
     * Constructor.
     * 
     * @param existingObject An existing Application Object for which a new version is to be created. 
     */
    public ApplicationObjectUpdater(IApplicationObjectPayload existingObject)
    {
      super(ApplicationObjectUpdater.class, existingObject.getStoredApplicationObject());
      
      IStoredApplicationObject existing = existingObject.getStoredApplicationObject();
      
      builder_
          .withPartitionHash(existing.getPartitionHash())
          .withSortKey(existing.getSortKey())
          .withOwner(getUserId())
          .withThreadId(existing.getThreadId())
          .withHeader(existing.getHeader())
          .withPurgeDate(existing.getPurgeDate())
          .withBaseHash(existing.getBaseHash())
          .withPrevHash(existing.getAbsoluteHash())
          .withPrevSortKey(existing.getSortKey())
          ;
      
      builder_.withPrevHash(existingObject.getStoredApplicationObject().getAbsoluteHash());
      builder_.withPrevSortKey(existingObject.getStoredApplicationObject().getSortKey());
    }

    @Override
    protected StoredApplicationObject construct()
    {
      // Nasty cast here, can't easily be avoided and it's quite safe but nonetheless.....
      return (StoredApplicationObject) builder_.build();
    }
  }
  

  
  /**
   * Builder for application type FundamentalObjects which takes an existing ApplicationObject for which a new
   * version is to be created.
   * 
   * @author Bruce Skingle
   *
   */
  public class ApplicationObjectDeleter extends EntityBuilder<ApplicationObjectDeleter, DeletedApplicationObject>
  {
    private DeletedApplicationObject.Builder builder_;
    
    /**
     * Constructor.
     * 
     * @param existingObject An existing Application Object for which a new version is to be created. 
     */
    public ApplicationObjectDeleter(IApplicationObjectPayload existingObject)
    {
      super(ApplicationObjectDeleter.class, existingObject.getStoredApplicationObject());
      
      IStoredApplicationObject existing = existingObject.getStoredApplicationObject();
      
      builder_ = new DeletedApplicationObject.Builder()
          .withPartitionHash(existing.getPartitionHash())
          .withSortKey(existing.getSortKey())
          .withOwner(getUserId())
          .withPurgeDate(existing.getPurgeDate())
          .withBaseHash(existing.getBaseHash())
          .withPrevHash(existing.getAbsoluteHash())
          .withPrevSortKey(existing.getSortKey())
          ;
    }
    
    /**
     * Set the deletion type.
     * 
     * @param value The deletion type.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectDeleter withDeletionType(DeletionType value)
    {
      builder_.withDeletionType(value);
      
      return self();
    }
    
    /**
     * Set the purge date for this object.
     * 
     * This is meaningless in the case of a physical delete but makes sense for a Logical Delete.
     * 
     * @param purgeDate The date after which this object may be deleted by the system.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectDeleter withPurgeDate(Instant purgeDate)
    {
      builder_.withPurgeDate(purgeDate);
      
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
    
    /**
     * Set the sort key for the object.
     * 
     * @param sortKey The sort key to be attached to this object within its partition.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectDeleter withSortKey(SortKey sortKey)
    {
      builder_.withSortKey(sortKey);
      
      return self();
    }
    
    /**
     * Set the sort key for the object.
     * 
     * @param sortKey The sort key to be attached to this object within its partition.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectDeleter withSortKey(String sortKey)
    {
      builder_.withSortKey(sortKey);
      
      return self();
    }
    
    @Override
    protected void validate()
    {
      if(builder_.getHashType() == null)
        builder_.withHashType(HashType.newBuilder().build(Hash.getDefaultHashTypeId()));
      
      if(builder_.getDeletionType() == null)
        throw new IllegalStateException("DeletionType is required.");
      
      builder_.withOwner(getUserId());
      
      super.validate();
    }

    @Override
    protected DeletedApplicationObject construct()
    {
      // Nasty cast here, can't easily be avoided and it's quite safe but nonetheless.....
      return (DeletedApplicationObject) builder_.build();
    }
  }
  
  @Override
  public void fetchPartitionObjects(FetchPartitionObjectsRequest request)
  {
    Hash partitionHash = request.getHash(getUserId());
    
    try(ITraceContextTransaction traceTransaction = traceContextFactory_.createTransaction("fetchSequence", partitionHash.toString()))
    {
      ITraceContext trace = traceTransaction.open();
      
       String after = request.getAfter();
       Integer limit = request.getMaxItems();
       
       int remainingItems = limit == null ? 0 : limit;
       
       do
       {
         PartitionsPartitionHashPageGetHttpRequestBuilder pageRequest = objectApiClient_.newPartitionsPartitionHashPageGetHttpRequestBuilder()
             .withPartitionHash(partitionHash)
             .withAfter(after)
             .withSortKeyPrefix(request.getSortKeyPrefix())
             .withScanForwards(request.getScanForwards())
             ;
         
         if(limit != null)
           pageRequest.withLimit(remainingItems);
         
         IPageOfStoredApplicationObject page = pageRequest
             .build()
             .execute(httpClient_);
         
         for(IAbstractStoredApplicationObject item : page.getData())
         {
           try
           {
             request.getConsumerManager().consume(item, trace, this);
           }
           catch (RetryableConsumerException | FatalConsumerException e)
           {
             request.getConsumerManager().consumeUnprocessable(item, trace, "Failed to process message", e);
           }
           remainingItems--;
         }
         
         after = null;
         IPagination pagination = page.getPagination();
         
         if(pagination != null)
         {
           ICursors cursors = pagination.getCursors();
           
           if(cursors != null)
             after = cursors.getAfter();
         }
       } while(after != null && (limit==null || remainingItems>0));
    }
  }
  
  @Override
  public ChatMessage.Builder newChatMessageBuilder()
  {
    return new ChatMessage.Builder().withRegistry(modelRegistry_, dataProvider_);
  }

  @Override
  public void sendMessage(IChatMessage chatMessage)
  {
    MessageMLContext context = new MessageMLContext(dataProvider_);
    
    String version = null;
    
    try
    {
      context.parseMessageML(chatMessage.getPresentationML().getValue(), chatMessage.getEntityJson().toString(), version);
      
      boolean dlpEnforceExpressionFiltering = false;
      
      JsonNode socialMessage = messageTramnsformer_.createSocialMessage(context, chatMessage.getThreadId().toBase64String(), dlpEnforceExpressionFiltering);
      
      JsonNode encryptedSocialMessageNode = agentEncryptionHandler_.handleEncrypt(chatMessage.getThreadId(), socialMessage,
          chatMessage.getPresentationML().getValue());
      
      IImmutableJsonDomNode encryptedSocialMessage = JacksonAdaptor.adapt(encryptedSocialMessageNode).immutify();
      
      podInternalApiClient_.newWebcontrollerIngestorV2MessageServicePostHttpRequestBuilder()
        .withMessagepayload(encryptedSocialMessage.toString())
        .build()
        .execute(httpClient_);
    }
    catch (InvalidInputException | ProcessingException | IOException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  private MutableJsonObject parseOneJsonObject(String json)
  {
    try
    {
      JsonNode tree = AUTO_CLOSE_MAPPER.readTree(new StringReader(json));
      
      IJsonDomNode adapted = JacksonAdaptor.adapt(tree);
      
      if(adapted instanceof MutableJsonObject)
      {
        return (MutableJsonObject) adapted;
      }
      else
      {
        throw new IllegalArgumentException("Expected a JSON Object but read a " + adapted.getClass().getName());
      }
    }
    catch(IOException e)
    {
      throw new IllegalArgumentException("Failed to parse input", e);
    }
  }

  @Override
  public IApplicationObjectPayload open(IStoredApplicationObject storedApplicationObject)
  {
    return cryptoClient_.decrypt(storedApplicationObject);
  }

  /**
   * Parse SocialMessage text. For MessageMLV2 messages, returns the PresentationML content. For legacy messages, parses
   * the Markdown content and JSON entities and returns their PresentationML representation.
   */
  @Override
  public IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message)
  {
    if(message instanceof ISocialMessage)
      return decryptSocialMessage((ISocialMessage) message);
    
    if(message instanceof IMaestroMessage)
    {
      return buildMaestroMessage((IMaestroMessage) message);
    }
    
    ReceivedChatMessage.Builder builder = new ReceivedChatMessage.Builder()
        .withMessageId(message.getMessageId())
        .withThreadId(message.getThreadId())
        ;
    
    String text = message.getVersion() + " message";
    
    return builder.withPresentationML(text)
        .withText(text)
        .withMarkDown(text)
        .build();
  }
  
  private IReceivedChatMessage buildMaestroMessage(IMaestroMessage message)
  {
    ReceivedMaestroMessage.Builder builder = new ReceivedMaestroMessage.Builder()
        .withMessageId(message.getMessageId())
        .withThreadId(message.getThreadId())
        ;
    
    String text = "";
    
    switch(message.getEvent())
    {
      case JOIN_ROOM:
        text = getUsers(message) + " joined the chat.";
        break;
        
      case LEAVE_ROOM:
        text = getUsers(message) + " left the chat.";
        break;
        
      default:
        text = message.getVersion() + " " + message.getEvent() + " message";
    }
    
    return builder.withPresentationML(text)
        .withText(text)
        .withMarkDown(text)
        .withMaestroMessage(message)
        .build();
  }

  private String getUsers(IMaestroMessage message)
  {
    int cnt = message.getAffectedUsers().size();
    
    if(cnt == 0)
      return "An unknown user";
    
    if(cnt == 1)
      return  message.getAffectedUsers().get(0).getPrettyName();
    
    StringBuilder b = new StringBuilder();
    int i=0;
    
    while(i<cnt - 1)
    {
      if(i>0)
        b.append(", ");
      
      b.append(message.getAffectedUsers().get(i++).getPrettyName());
    }
    
    b.append(" and ");
    b.append(message.getAffectedUsers().get(i).getPrettyName());
    
    return b.toString();
  }

  private IReceivedSocialMessage decryptSocialMessage(ISocialMessage message)
  {
    if(FORMAT_MESSAGEMLV2.equals(message.getFormat()))
    {
      String presentationML       = cryptoClient_.decrypt(message.getThreadId(), message.getPresentationML());
      String encryptedEntityJson  = message.getEntityJSON();
      String entityJsonString;
      
      if(encryptedEntityJson == null)
      {
        entityJsonString = "{}";
      }
      else
      {
        entityJsonString = cryptoClient_.decrypt(message.getThreadId(), encryptedEntityJson);
        
        
      }
      
      String text = null;
      String markDown = null;
      
      if(message.getText() != null)
      {
        markDown = cryptoClient_.decrypt(message.getThreadId(), message.getText());
      }
      
      MessageML messageML;
      try
      {
        MessageMLContext context = new MessageMLContext(dataProvider_);
        
        String version = null;
        
        context.parseMessageML(presentationML, entityJsonString, version);
        
        text = context.getText(false);
        
        if(markDown == null)
          markDown = context.getMarkdown();
        
        messageML = context.getMessageML();
      }
      catch (InvalidInputException | ProcessingException | IOException e)
      {
        log_.error("Unable to decode text from messageML");
        messageML = null;
      }
      
      if(markDown == null)
      {
        // We are desperate now...
        
        markDown = presentationML.replaceAll("<[^>]*>", "");
      }
      
      if(text == null)
        text = markDown;
      
      ReceivedSocialMessage.Builder builder = new ReceivedSocialMessage.Builder()
          .withMessageId(message.getMessageId())
          .withThreadId(message.getThreadId())
          .withPresentationML(presentationML)
          .withText(text)
          .withMarkDown(markDown)
          .withEntityJson(new EntityJson(parseOneJsonObject(entityJsonString), modelRegistry_))
          .withMessageML(messageML)
          .withSocialMessage(message)
          ;
      
      return builder
        .build()
        ;
    }
    else
    {
      try
      {
        // TODO: re-factor messageML utils to allow us to get rid of this
        JsonNode messageJson = OBJECT_MAPPER.readTree(message.toString());
        
        //If this check works, it also means the input is an ObjectNode
        JsonNode textNode = messageJson.get("text");
        if (textNode == null)
        {
          throw new IllegalArgumentException("Message text node was null.");
        }
        
        String clearText = null;
        String clearPresentationML = null;
        String clearEntityJson = null;
        String clearMedia = null;
        String clearEntities = null;
        
        
        ObjectNode decryptedNode = new ObjectNode(JsonNodeFactory.instance);

        for(Iterator<Map.Entry<String, JsonNode>> it = messageJson.fields(); it.hasNext(); )
        {
          Map.Entry<String, JsonNode> field = it.next();
          String nodeName = field.getKey();
          JsonNode node = field.getValue();

          switch (nodeName)
          {
            case "encryptedEntities":
              //
              // EncryptedEntities are encrypted with the content key.
              //
              clearEntities = cryptoClient_.decrypt(message.getThreadId(), node.asText());
              JsonNode clearEntitiesJson = OBJECT_MAPPER.readTree(clearEntities);
              decryptedNode.set("entities", clearEntitiesJson);
              break;
              
            case "encryptedMedia":
              clearMedia = cryptoClient_.decrypt(message.getThreadId(), node.asText());
              JsonNode clearMediaJson = OBJECT_MAPPER.readTree(clearMedia);
              decryptedNode.set("media", clearMediaJson);
              break;
              
            case "text":
              clearText = cryptoClient_.decrypt(message.getThreadId(), node.asText());
              decryptedNode.put("text", clearText);
              break;
              
            case "presentationML":
              clearPresentationML = cryptoClient_.decrypt(message.getThreadId(), node.asText());
              decryptedNode.put("presentationML", clearPresentationML);
              break;
              
            case "entityJSON":
              clearEntityJson = cryptoClient_.decrypt(message.getThreadId(), node.asText());
              decryptedNode.put("entityJSON", clearEntityJson);
              break;
              
            case "tokenIds":
              break;
              
            case "msgFeatures":
              int msgFeaturesInt = (node != null) ? node.asInt() : 0;
              int newMsgFeatures = mediaEncryptionOff(encryptionOff(msgFeaturesInt));
              decryptedNode.put("msgFeatures", newMsgFeatures);
              break;
              
            default:
              decryptedNode.set(nodeName, node);
          }
        }
        
        
        JsonNode entities = messageJson.path("entities");
        JsonNode media = messageJson.path("media");
    
        MessageMLContext context = new MessageMLContext(dataProvider_);
        
        
        context.parseMarkdown(clearText, entities, media);

        MessageML messageML = context.getMessageML();
        
        if (message.getIsChime())
        {
          Chime chime = new Chime(messageML, FormatEnum.PRESENTATIONML);
          messageML.addChild(chime);
        }
        
        String text;
        try
        {
          text = context.getText(false);
        }
        catch (ProcessingException | IllegalStateException e)
        {
          text = clearText;
        }

        //return new ChatMessage(message.getThreadId(), context.getPresentationML(), context.getEntityJson().asText());
        
        ReceivedSocialMessage.Builder builder = new ReceivedSocialMessage.Builder()
            .withMessageId(message.getMessageId())
            .withThreadId(message.getThreadId())
            .withPresentationML(context.getPresentationML())
            .withMessageML(messageML)
            .withText(text)
            .withMarkDown(clearText)
            .withSocialMessage(message)
            ;
            
            String      encryptedEntityJson = context.getEntityJson().asText();
            
            if(encryptedEntityJson != null)
            {
              String jsonString = cryptoClient_.decrypt(message.getThreadId(), encryptedEntityJson);
              
              builder.withEntityJson(new EntityJson(parseOneJsonObject(jsonString), modelRegistry_));
            }
            
            return builder
              .build()
              ;
      }
      catch(IOException e)
      {
        throw new CodingFault("In memory io", e);
      }
      catch (InvalidInputException e)
      {
        throw new IllegalArgumentException(e);
      }
    }
  }


  private int encryptionOff(int msgFeatures) {
    int featureMask = 1 << ENCRYPTION_ORDINAL;
    return msgFeatures & ~featureMask;
  }

  private int mediaEncryptionOff(int msgFeatures) {
    int featureMask = 1 << MEIDA_ENCRYPTION_ORDINAL;
    return msgFeatures & ~featureMask;
  }


  @Override
  public IAllegroApi self()
  {
    return this;
  }

  /**
   * The builder implementation.
   * 
   * This is implemented as an abstract class to allow for sub-classing in future.
   * 
   * Any sub-class of AllegroApi would need to implement its own Abstract sub-class of this class
   * and then a concrete Builder class which is itself a sub-class of that.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The type of the concrete Builder
   * @param <B> The type of the built class, some subclass of AllegroApi
   */
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends IAllegroApi>
  extends BaseAbstractBuilder<T, B>
  {
    private PrivateKey                      rsaCredential_;

    protected CipherSuiteId                 cipherSuiteId_;
    protected ICipherSuite                  cipherSuite_;
    protected PemPrivateKey                 rsaPemCredential_;
    protected CloseableHttpClient           httpclient_;
    protected URL                           podUrl_;
    protected URL                           objectStoreUrl_;
    protected String                        userName_;
    protected CookieStore                   cookieStore_;
    protected List<IEntityFactory<?, ?, ?>> factories_ = new LinkedList<>();
    protected List<X509Certificate>         trustedCerts_ = new LinkedList<>();
    protected List<String>                  trustedCertResources_ = new LinkedList<>();
    private TrustStrategy                   sslTrustStrategy_     = null;

    public AbstractBuilder(Class<T> type)
    {
      super(type);
      
      try
      {
        objectStoreUrl_ = new URL("https://api.symphony.com");
      }
      catch (MalformedURLException e)
      {
        throw new IllegalArgumentException("Invalid default URL", e);
      }
    }
    
    public T withUserName(String serviceAccountName)
    {
      userName_ = serviceAccountName;
      
      return self();
    }

    public T withPodUrl(URL podUrl)
    {
      podUrl_ = podUrl;
      
      return self();
    }

    public T withPodUrl(String podUrl)
    {
      try
      {
        podUrl_ = new URL(podUrl);
      }
      catch (MalformedURLException e)
      {
        throw new IllegalArgumentException("Invalid podUrl", e);
      }
      
      return self();
    }

    public T withObjectStoreUrl(URL objectStoreUrl)
    {
      objectStoreUrl_ = objectStoreUrl;
      
      return self();
    }

    public T withObjectStoreUrl(String objectStoreUrl)
    {
      try
      {
        objectStoreUrl_ = new URL(objectStoreUrl);
      }
      catch (MalformedURLException e)
      {
        throw new IllegalArgumentException("Invalid objectStoreUrl", e);
      }
      
      return self();
    }
    
    public T withCipherSuite(String cipherSuiteId)
    {
      CipherSuiteId id = CipherSuiteId.valueOf(cipherSuiteId);
      
      if(id == null)
        throw new IllegalArgumentException("Invalid cipher suite ID \"" + cipherSuiteId + "\"");
      
      cipherSuiteId_ = id;
      
      return self();
    }
    
    public T withRsaPemCredential(PemPrivateKey rsaPemCredential)
    {
      rsaPemCredential_ = rsaPemCredential;
      
      return self();
    }
    
    public T withRsaPemCredential(String rsaPemCredential)
    {
      checkCredentialNotNull(rsaPemCredential);
      
      rsaPemCredential_ = PemPrivateKey.newBuilder().build(rsaPemCredential);
      
      return self();
    }
    
    private void checkCredentialNotNull(Object credential)
    {
      if(credential == null)
        throw new IllegalArgumentException("Credential is required");
    }

    public T withRsaCredential(PrivateKey rsaCredential)
    {
      checkCredentialNotNull(rsaCredential);
      
      rsaCredential_ = rsaCredential;
      
      return self();
    }
    
    public T withRsaPemCredentialFile(String rsaPemCredentialFile)
    {
      checkCredentialNotNull(rsaPemCredentialFile);
      
      File file = new File(rsaPemCredentialFile);
      
      if(!file.canRead())
        throw new IllegalArgumentException("Credential file is unreadable");
      
      try
      {
        rsaPemCredential_ = PemPrivateKey.newBuilder().build(new String(Files.toByteArray(file)));
      }
      catch (IOException e)
      {
        throw new IllegalArgumentException("Unable to read credential file.", e);
      }
      
      return self();
    }
    
    public T withTrustAllSslCerts()
    {
      sslTrustStrategy_ = new TrustAllStrategy();
      
      return self();
    }
    
    public T withTrustSelfSignedSslCerts()
    {
      sslTrustStrategy_ = new TrustSelfSignedStrategy();
      
      return self();
    }
    
    public T withTrustedSslCertResources(String ...resourceNames)
    {
      for(String resourceName : resourceNames)
      {
        trustedCertResources_.add(resourceName);
      }
      return self();
    }
    
    public T withFactories(IEntityFactory<?, ?, ?>... factories)
    {
      if(factories != null)
      {
        for(IEntityFactory<?, ?, ?> factory : factories)
          factories_.add(factory);
      }
      
      return self();
    }

    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      if(rsaCredential_ == null && rsaPemCredential_ == null)
        faultAccumulator.error("rsaCredential is required");
      
      cipherSuite_ = cipherSuiteId_ == null ? CipherSuite.getDefault() : CipherSuite.get(cipherSuiteId_);
      
      rsaCredential_ = cipherSuite_.privateKeyFromPem(rsaPemCredential_);
      
      for(String resourceName : trustedCertResources_)
      {
        trustedCerts_.add(cipherSuite_.certificateFromPemResource(resourceName));
      }
      
      cookieStore_ = new BasicCookieStore();
      
      HttpClientBuilder httpBuilder = HttpClients.custom().setDefaultCookieStore(cookieStore_);
      
      if(!trustedCerts_.isEmpty() || sslTrustStrategy_ != null)
      {
        configureTrust(httpBuilder);
      }
      
      httpclient_ = httpBuilder.build();
    }
    
    private void configureTrust(HttpClientBuilder httpBuilder)
    {
      try
      {
        KeyStore trustStore  = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null);
        
        int n=1;
        
        for(X509Certificate trustedCert : trustedCerts_)
        {
          trustStore.setCertificateEntry("cert" + n++, trustedCert);
        }
        
        // Trust own CA and all self-signed certs
        SSLContext sslcontext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(trustStore, sslTrustStrategy_)
                .build();

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslcontext,
                null,
                null,
                (HostnameVerifier)null);
        
        httpBuilder.setSSLSocketFactory(sslsf);
      }
      catch(GeneralSecurityException | IOException e)
      {
        throw new IllegalStateException("Failed to configure SSL trust", e);
      }
    }
  }
  
  /**
   * Builder for AllegroApi.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, IAllegroApi>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected IAllegroApi construct()
    {
      return new AllegroApi(this);
    }
  }
}
