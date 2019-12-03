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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.symphonyoss.s2.canon.runtime.IEntityFactory;
import org.symphonyoss.s2.canon.runtime.IModelRegistry;
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
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.core.trace.NoOpContextFactory;
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
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectUpdater;
import com.symphony.oss.allegro.api.agent.util.EncryptionHandler;
import com.symphony.oss.allegro.api.agent.util.V4MessageTransformer;
import com.symphony.oss.allegro.api.auth.AuthHandler;
import com.symphony.oss.allegro.api.request.FetchPartitionObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchPartitionRequest;
import com.symphony.oss.allegro.api.request.FetchRecentMessagesRequest;
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
import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.core.canon.ICursors;
import com.symphony.oss.models.core.canon.IPagination;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.core.canon.facade.UserId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
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
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;
import com.symphony.oss.models.object.canon.NamedUserIdObject;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;
import com.symphony.oss.models.object.canon.ObjectModel;
import com.symphony.oss.models.object.canon.PartitionsPartitionHashPageGetHttpRequestBuilder;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.SortKey;
import com.symphony.oss.models.object.canon.facade.StoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.StoredApplicationObject.AbstractStoredApplicationObjectBuilder;
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
  
  private static final Logger                   log_                       = LoggerFactory.getLogger(AllegroApi.class);

  private static final String                   FORMAT_MESSAGEMLV2         = "com.symphony.messageml.v2";
  private static final ObjectMapper             OBJECT_MAPPER              = new ObjectMapper();  // TODO: get rid of this
  private static final int                      ENCRYPTION_ORDINAL         = 0;
  private static final int                      MEIDA_ENCRYPTION_ORDINAL   = 2;
  
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
  public void store(IStoredApplicationObject object)
  {
    objectApiClient_.newObjectsObjectHashPutHttpRequestBuilder()
      .withObjectHash(object.getAbsoluteHash())
      .withCanonPayload(object)
      .build()
      .execute(httpClient_);
  }
//
//  @Override
//  public IFundamentalObject store(IFundamentalId id)
//  {
//    IFundamentalObject fundamentalObject = new FundamentalObject.IdBuilder()
//        .withFundamentalId(id)
//        .build();
//    
//    store(fundamentalObject);
//    
//    return fundamentalObject;
//  }
//
//  @Override
//  public ISequence fetchSequenceMetaData(FetchSequenceMetaDataRequest request)
//  {
//    request.validate();
//    
//    Hash subjectHash = request.getPrincipalBaseHash();
//    
//    if(subjectHash == null)
//      subjectHash = getPrincipalBaseHash();
//    
//    IContentIdObject id = new ContentIdObject.Builder()
//        .withSubjectHash(subjectHash)
//        .withSubjectType(Principal.CLIENT_TYPE_ID)
//        .withContentType(request.getContentType())
//        .withIdType(request.getSequenceType() == SequenceType.ABSOLUTE ? ContentIdType.ABSOLUTE_SEQUENCE : ContentIdType.CURRENT_SEQUENCE)
//        .build();
//    
//    return fetchCurrent(id.getAbsoluteHash(), ISequence.class);
//
////    IFundamentalObject result = fundamentalApiClient_.newObjectsObjectHashGetHttpRequestBuilder()
////        .withCurrentVersion(true)
////        .withObjectHash(id.getAbsoluteHash())
////        .build()
////        .execute(httpClient_);
////    
////    if(result.getPayload() instanceof IdPlainTextPayloadContainer)
////    {
////      IApplicationObject payload = ((IdPlainTextPayloadContainer)result.getPayload()).getPayload();
////      
////      if(!(payload instanceof ObjectPointer))
////        throw new IllegalStateException("Unexpected return type " + payload.getCanonType() + ", expected ObjectPointer.");
////      
////      Hash sequenceHash = ((ObjectPointer)payload).getBaseHash();
////      
////      return fetchAbsolute(sequenceHash, ISequence.class);
////    }
////    
////    throw new IllegalStateException("Unexpected return type " + result.getPayload().getCanonType() + ", expected IdPlainTextPayloadContainer.");
//
//  }
//  
//  @Override
//  public void storeCredential()
//  {
//    systemApiClient_.newCredentialsPostHttpRequestBuilder()
//        .withCanonPayload(new SaveCredentialRequest.Builder()
//            .withCipherSuiteId(cipherSuite_.getId())
//            .withEncodedPrivateKey(rsaPemCredential_)
//            .withUserName(userName_)
//            .build()
//            )
//        .build()
//        .execute(httpClient_)
//        ;
//  }
//  
//  @Override
//  public IFugueLifecycleComponent createFeedSubscriber(CreateFeedSubscriberRequest request)
//  {
//    AllegroSubscriberManager subscriberManager = new AllegroSubscriberManager.Builder()
//        .withHttpClient(httpClient_)
//        .withSystemApiClient(systemApiClient_)
//        .withTraceContextTransactionFactory(traceContextFactory_)
//        .withUnprocessableMessageConsumer(request.getUnprocessableMessageConsumer())
//        .withSubscription(new AllegroSubscription(request, this))
//        .withSubscriberThreadPoolSize(request.getSubscriberThreadPoolSize())
//        .withHandlerThreadPoolSize(request.getHandlerThreadPoolSize())
//      .build();
//    
//    return subscriberManager;
//  }
//  
//  @Override
//  public void fetchFeedMessages(FetchFeedMessagesRequest request)
//  {
//    try(ITraceContextTransaction traceTransaction = traceContextFactory_.createTransaction("FetchFeed", request.getName()))
//    {
//      ITraceContext trace = traceTransaction.open();
//      
//      List<IFeedMessage> messages  = systemApiClient_.newFeedsNameMessagesPostHttpRequestBuilder()
//          .withName(request.getName())
//          .withCanonPayload(new FeedRequest.Builder()
//              .withMaxMessages(request.getMaxMessages() != null ? request.getMaxMessages() : 1)
//              .build())
//          .build()
//          .execute(httpClient_);
//      
//      FeedRequest.Builder builder = new FeedRequest.Builder()
//          .withMaxMessages(0)
//          .withWaitTimeSeconds(0);
//      
//      System.out.println("Received " + messages.size() + " messages.");
//      for(IFeedMessage message : messages)
//      {
//        request.consume(message.getPayload(), trace, this);
//          
//        builder.withDelete(new FeedMessageDelete.Builder()
//            .withReceiptHandle(message.getReceiptHandle())
//            .build()
//            );
//      }
//      
//      try
//      {
//        // Delete (ACK) the consumed messages
//        messages = systemApiClient_.newFeedsNameMessagesPostHttpRequestBuilder()
//            .withName(request.getName())
//            .withCanonPayload(builder.build())
//            .build()
//            .execute(httpClient_);
//      }
//      catch(NotFoundException e)
//      {
//        messages.clear();
//      }
//    }
//    catch(NotFoundException e)
//    {
//      // No messages
//    }
//    
//    request.closeConsumers();
//  }
//
//  @Override
//  public IFeed upsertFeed(UpsertFeedRequest request)
//  {
//    request.validate();
//    
//    ISubscriptionRequest subscriptionRequest = new SubscriptionRequest.Builder()
//        .withType(request.getType())
//        .withSequences(request.getSequences())
//        .build()
//        ;
//    
//    IFeed feed = systemApiClient_.newFeedsNamePostHttpRequestBuilder()
//        .withName(request.getName())
//        .withCanonPayload(subscriptionRequest)
//        .build()
//        .execute(httpClient_)
//        ;
//    
//    return feed;
//  }
//
//  @Override
//  public void upsertGatewaySubscription(UpsertSmsGatewayRequest request)
//  {
////    String name = SmsGatewayMetadata.TYPE_ID + ":" + request.getType();
////    
////    ISubscriptionRequest subscriptionRequest = new SubscriptionRequest.Builder()
////        .withType(request.getType())
////        .withSequences(request.getSequences())
////        .build()
////        ;
////
////    IFeed feed = systemApiClient_.newFeedsNamePostHttpRequestBuilder()
////        .withName(name)
////        .withCanonPayload(subscriptionRequest)
////        .build()
////        .execute(httpClient_)
////        ;
//    
//    ISmsGatewayMetadata metaData = new SmsGatewayMetadata.Builder()
//        .withType(FeedType.EXTERNAL_GATEWAY)
//        .withSequences(request.getSequences())
//        .withCipherSuiteId(cipherSuite_.getId())
//        .withEncodedPrivateKey(rsaPemCredential_)
//        .withUserName(userName_)
//        .withPhoneNumber(request.getPhoneNumber())
//        .build()
//        ;
//    
//    ISignedApplicationObject metaDataObject = new SignedApplicationObject.Builder()
//        .withPayload(metaData)
//        .withSigningKey(cryptoClient_.getSigningKey())
//        .build();
//    
//    ISubscriptionMetadataRequest subscriptionMetadataRequest = new SubscriptionMetadataRequest.Builder()
//        .withMetadata(metaDataObject)
//        .withSequences(request.getSequences())
//        .build()
//        ;
//    
//    
//    systemApiClient_.newGatewaysMetadataPostHttpRequestBuilder()
//        .withCanonPayload(subscriptionMetadataRequest)
//        .build()
//        .execute(httpClient_)
//        ;
//  }

  @Override
  public Hash getPartitionHash(FetchPartitionRequest request)
  {
    return request.getPartitionHash();
  }
  
  @Override
  public IPartition upsertPartition(UpsertPartitionRequest request)
  {
    return objectApiClient_.newPartitionsUpsertPostHttpRequestBuilder()
      .withCanonPayload(new com.symphony.oss.models.object.canon.UpsertPartitionRequest.Builder()
          .withPartitionId(new NamedUserIdObject.Builder()
              .withUserId(getUserId())
              .withName(request.getName())
              .build())
            .withThreadIds(request.getThreadIds())
            .build())
      .build()
      .execute(httpClient_)
      ;
    
//    Hash subjectHash = request.getO;
//    
//    if(subjectHash == null)
//      subjectHash = getPrincipalBaseHash();
//    
//    IContentIdObject id = new ContentIdObject.Builder()
//        .withSubjectHash(subjectHash)
//        .withSubjectType(Principal.CLIENT_TYPE_ID)
//        .withContentType(request.getContentType())
//        .withIdType(request.getSequenceType() == SequenceType.ABSOLUTE ? ContentIdType.ABSOLUTE_SEQUENCE : ContentIdType.CURRENT_SEQUENCE)
//        .build();
//    
//    try
//    {
//      return fetchCurrent(id.getAbsoluteHash(), ISequence.class);
//    }
//    catch(NotFoundException e)
//    {
//      IOpenSimpleSecurityContext securityContext = cryptoClient_.getOrCreateThreadSecurityContext(request.getThreadId());
//
//      ISequence sequence = new Sequence.Builder()
//          .withType(request.getSequenceType())
//          .withSecurityContextHash(securityContext.getParentHash())
//          .withSigningKeyHash(cryptoClient_.getSigningKey().getAbsoluteHash())
//          .withBaseHash(id.getAbsoluteHash())
//          .withPrevHash(id.getAbsoluteHash())
//          .build();
//      
//      IFundamentalObject sequenceObject = new FundamentalObject.ObjectBuilder()
//          .withPayload(sequence)
//          .withSigningKey(cryptoClient_.getSigningKey())
//          .build();
//      
////      ITransaction transaction = new Transaction.Builder()
////        .withId(id)
////        .withAdditionalObjects(sequenceObject)
////        .build();
//      
//      IFundamentalObject idObject =  new FundamentalObject.IdBuilder()
//      .withFundamentalId(id)
//      .build();
//      
//      fundamentalApiClient_.newObjectsTransactionPostHttpRequestBuilder()
//        .withCanonPayload(idObject)
//        .withCanonPayload(sequenceObject)
//        .build()
//        .execute(httpClient_);
//      
////      store(id);
////      store(sequenceObject); we need to make these two calls in one and validate the ID object for principal hash
//      
//      return sequence;
//    }

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
  
  IUserV2 getUserInfo()
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
        request.getConsumerManager().consume(liveCurrentMessageFactory_.newLiveCurrentMessage(envelope.getMessage().getJsonObject().mutify(), modelRegistry_), trace, this);
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
//  private IOpenSecurityContext getPodSecurityContext(Hash securityContextHash)
//  {
//    IOpenSecurityContextInfo securityContextInfo = podPrivateApiClient_.newSecurityContextsObjectHashGetHttpRequestBuilder()
//        .withObjectHash(securityContextHash)
//        .build()
//        .execute(httpClient_);
//    
//    return new OpenSecurityContext(securityContextInfo, modelRegistry_);
//  }

  private IChatMessage maestroMessage(IMaestroMessage message)
  {
    ReceivedChatMessage.Builder builder = new ReceivedChatMessage.Builder()
        .withMessageId(message.getMessageId())
        .withThreadId(message.getThreadId())
        .withPresentationML(message.getEvent() + " " + message.getVersion() + " message");
        ;
        
    return builder.build();
  }

//  @Override
//  public IEntity open(IFundamentalObject item, ThreadId threadId)
//  {
//    return doOpen(item, threadId);
//  }
//
//  @Override
//  public IEntity open(IFundamentalObject item)
//  {
//    return doOpen(item, null);
//  }
//
//  private IEntity doOpen(IFundamentalObject item, @Nullable ThreadId threadId)
//  {
//    IFundamentalPayload payload = item.getPayload();
//    
//    IBlob blob;
//    
////    if(payload instanceof IIdPayloadContainer)
////    {
////      blob = ((IIdPayloadContainer) payload).getPayload();
////    }
////    else 
//    if(payload instanceof IBlob)
//    {
//      blob = (IBlob)payload;
//    }
//    else if(payload instanceof IClob)
//    {
//      return ((IClob)payload).getPayload();
//    }
//    else
//    {
//      return payload;
//    }
//    
//    IOpenSimpleSecurityContext securityContext = cryptoClient_.getSecurityContext(blob.getSecurityContextHash(), threadId);
//    
//    return modelRegistry_.open(item, securityContext);
//  }
  
  @Override
  public ApplicationObjectBuilder newStoredApplicationObjectBuilder()
  {
    return new ApplicationObjectBuilder();
  }
  
  /**
   * Builder for FundamentalObjects which accepts a ThreadId in place of a security context and which attaches the podId.
   * 
   * @author Bruce Skingle
   *
   */
  public class ZZApplicationObjectBuilder extends AbstractStoredApplicationObjectBuilder<ApplicationObjectBuilder, IStoredApplicationObject>
  implements IEncryptableObjectBuilder
  {
    private IApplicationPayload payload_;

    /**
     * Constructor.
     */
    public ZZApplicationObjectBuilder()
    {
      super(ZZApplicationObjectBuilder.class);
    }
    
    /**
     * Set the object payload (which is to be encrypted).
     * 
     * @param payload The object payload (which is to be encrypted).
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withPayload(IApplicationPayload payload)
    {
      payload_ = payload;
      
      return self();
    }

    /**
     * 
     * @return the unencrypted payload.
     */
    @Override
    public IApplicationPayload getPayload()
    {
      return payload_;
    }

    @Override
    protected void validate()
    {
      if(getHashType() == null)
        withHashType(HashType.newBuilder().build(Hash.getDefaultHashTypeId()));
      
      if(getThreadId() == null)
        throw new IllegalStateException("ThreadId is required.");
      
      if(payload_ == null)
        throw new IllegalStateException("Payload is required.");
      
      withOwner(getUserId());
      
      cryptoClient_.encrypt(this);
      
      super.validate();
    }

    @Override
    public ApplicationObjectBuilder withEncryptedPayload(EncryptedData value)
    {
      throw new IllegalArgumentException("Call withPayload() instead.");
    }

    @Override
    public ApplicationObjectBuilder withEncryptedPayload(ImmutableByteArray value)
    {
      throw new IllegalArgumentException("Call withPayload() instead.");
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
      withPartitionHash(partition.getId().getHash());
      
      return this;
    }

    @Override
    protected IStoredApplicationObject construct()
    {
      return new StoredApplicationObject(this);
    }

    /**
     * Package private access to set encryptedPayload from IAllegroCryptoClient.encrypt().
     * 
     * @param encryptedPayload The encrypted payload.
     */
    void setEncryptedPayload(EncryptedData encryptedPayload)
    {
      super.withEncryptedPayload(encryptedPayload);
    }

  }
  
//  @Override
//  public void delete(IFundamentalObject existingObject, DeletionType deletionType)
//  {
//    if(!(existingObject.getPayload() instanceof IVersionedObject))
//      throw new BadRequestException("Only versioned objects may be deleted.");
//    
//    IFundamentalObject toDoObject = new FundamentalObject.VersionedObjectDeleter((IVersionedObject) existingObject.getPayload())
//        .withDeletionType(deletionType)
//        .build();
//    
//    store(toDoObject);
//  }

  @Override
  public ApplicationObjectUpdater newApplicationObjectUpdater(IApplicationObjectPayload existingObject)
  {
    return new ApplicationObjectUpdater(existingObject);
  }
  
  /**
   * Builder for application type FundamentalObjects which takes an existing ApplicationObject for which a new
   * version is to be created.
   * 
   * @author Bruce Skingle
   *
   */
  public class ApplicationObjectUpdater extends AbstractStoredApplicationObjectBuilder<ApplicationObjectUpdater, IStoredApplicationObject>
  implements IEncryptableObjectBuilder
  {
    private static final String NOT_ALLOWED = "This method cannot be called for an Updater";
    
    private IApplicationPayload payload_;

    /**
     * Constructor.
     * 
     * @param existingObject An existing Application Object for which a new version is to be created. 
     */
    public ApplicationObjectUpdater(IApplicationObjectPayload existingObject)
    {
      super(ApplicationObjectUpdater.class, existingObject.getStoredApplicationObject());
      
//      super.withBaseHash(existingObject.getStoredApplicationObject().getBaseHash());
      super.withPrevHash(existingObject.getStoredApplicationObject().getAbsoluteHash());
      super.withPrevSortKey(existingObject.getStoredApplicationObject().getSortKey());
    }
    
    /**
     * Set the object payload (which is to be encrypted).
     * 
     * @param payload The object payload (which is to be encrypted).
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectUpdater withPayload(IApplicationPayload payload)
    {
      payload_ = payload;
      
      return self();
    }

    /**
     * 
     * @return the unencrypted payload.
     */
    public IApplicationPayload getPayload()
    {
      return payload_;
    }

    @Override
    public ApplicationObjectUpdater withValues(ImmutableJsonObject jsonObject, boolean ignoreValidation,
        IModelRegistry modelRegistry)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withPartitionHash(Hash value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withPartitionHash(ImmutableByteArray value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withBaseHash(Hash value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withBaseHash(ImmutableByteArray value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withPrevHash(Hash value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withPrevHash(ImmutableByteArray value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withPrevSortKey(SortKey value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withPrevSortKey(String value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withCipherSuiteId(CipherSuiteId value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withCipherSuiteId(String value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withThreadId(ThreadId value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withThreadId(ImmutableByteArray value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withRotationId(RotationId value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    public ApplicationObjectUpdater withRotationId(Long value)
    {
      throw new IllegalArgumentException(NOT_ALLOWED);
    }

    @Override
    protected void validate()
    {
      if(getHashType() == null)
        withHashType(HashType.newBuilder().build(Hash.getDefaultHashTypeId()));
      
      if(getThreadId() == null)
        throw new IllegalStateException("ThreadId is required.");
      
      if(payload_ == null)
        throw new IllegalStateException("Payload is required.");
      
      withOwner(getUserId());
      
      cryptoClient_.encrypt(this);
      
      super.validate();
    }

    @Override
    protected IStoredApplicationObject construct()
    {
      return new StoredApplicationObject(this);
    }

  }
  
//  @Override
//  public IPageOfFundamentalObject fetchSequencePage(IFundamentalId sequenceId, Integer limit, String after)
//  {
//     return fundamentalApiClient_.newSequencesSequenceHashPageGetHttpRequestBuilder()
//      .withSequenceHash(sequenceId.getAbsoluteHash())
//      .withLimit(limit)
//      .withAfter(after)
//      .build()
//      .execute(httpClient_);
//  }
//  
//  @Override
//  @Deprecated
//  public void fetchSequence(FetchSequenceRequest request, Consumer<IFundamentalObject> consumer)
//  {
//     String after = request.getAfter();
//     Integer limit = request.getMaxItems();
//     
//     if(limit != null && limit < 1)
//       throw new BadRequestException("Limit must be at least 1 or not specified");
//     
//     int remainingItems = limit == null ? 0 : limit;
//     
//     do
//     {
//       SequencesSequenceHashPageGetHttpRequestBuilder pageRequest = fundamentalApiClient_.newSequencesSequenceHashPageGetHttpRequestBuilder()
//           .withSequenceHash(request.getSequenceHash())
//           .withAfter(after)
//           ;
//       
//       if(limit != null)
//         pageRequest.withLimit(remainingItems);
//       
//       IPageOfFundamentalObject page = pageRequest
//           .build()
//           .execute(httpClient_);
//       
//       for(IFundamentalObject item : page.getData())
//       {
//         consumer.accept(item);
//         remainingItems--;
//       }
//       
//       after = null;
//       IPagination pagination = page.getPagination();
//       
//       if(pagination != null)
//       {
//         ICursors cursors = pagination.getCursors();
//         
//         if(cursors != null)
//           after = cursors.getAfter();
//       }
//     } while(after != null && (limit==null || remainingItems>0));
//  }
  
  @Override
  public void fetchPartitionObjects(FetchPartitionObjectsRequest request)
  {
    try(ITraceContextTransaction traceTransaction = traceContextFactory_.createTransaction("fetchSequence", request.getPartitionHash().toString()))
    {
      ITraceContext trace = traceTransaction.open();
      
       String after = request.getAfter();
       Integer limit = request.getMaxItems();
       
       int remainingItems = limit == null ? 0 : limit;
       
       do
       {
         PartitionsPartitionHashPageGetHttpRequestBuilder pageRequest = objectApiClient_.newPartitionsPartitionHashPageGetHttpRequestBuilder()
             .withPartitionHash(request.getPartitionHash())
             .withAfter(after)
             ;
         
         if(limit != null)
           pageRequest.withLimit(remainingItems);
         
         IPageOfStoredApplicationObject page = pageRequest
             .build()
             .execute(httpClient_);
         
         for(IStoredApplicationObject item : page.getData())
         {
           request.getConsumerManager().consume(item, trace, this);
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
