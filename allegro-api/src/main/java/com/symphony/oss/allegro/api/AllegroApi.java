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
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.canon.runtime.EntityBuilder;
import org.symphonyoss.s2.canon.runtime.IEntity;
import org.symphonyoss.s2.common.dom.json.IImmutableJsonDomNode;
import org.symphonyoss.s2.common.dom.json.IJsonDomNode;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonObject;
import org.symphonyoss.s2.common.dom.json.MutableJsonObject;
import org.symphonyoss.s2.common.dom.json.jackson.JacksonAdaptor;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
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
import com.symphony.oss.allegro.api.agent.util.EncryptionHandler;
import com.symphony.oss.allegro.api.agent.util.V4MessageTransformer;
import com.symphony.oss.allegro.api.auth.AuthHandler;
import com.symphony.oss.allegro.api.request.FetchFeedMessagesRequest;
import com.symphony.oss.allegro.api.request.FetchRecentMessagesRequest;
import com.symphony.oss.allegro.api.request.PartitionId;
import com.symphony.oss.model.chat.LiveCurrentMessageFactory;
import com.symphony.oss.models.allegro.canon.EntityJson;
import com.symphony.oss.models.allegro.canon.facade.ChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedMaestroMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedSocialMessage;
import com.symphony.oss.models.allegro.canon.facade.ReceivedChatMessage;
import com.symphony.oss.models.allegro.canon.facade.ReceivedMaestroMessage;
import com.symphony.oss.models.allegro.canon.facade.ReceivedSocialMessage;
import com.symphony.oss.models.chat.canon.ChatModel;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.chat.canon.IMaestroMessage;
import com.symphony.oss.models.chat.canon.facade.ISocialMessage;
import com.symphony.oss.models.core.canon.HashType;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.core.canon.facade.UserId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.CryptoModel;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.crypto.cipher.ICipherSuite;
import com.symphony.oss.models.internal.km.canon.KmInternalHttpModelClient;
import com.symphony.oss.models.internal.km.canon.KmInternalModel;
import com.symphony.oss.models.internal.pod.canon.AckId;
import com.symphony.oss.models.internal.pod.canon.FeedId;
import com.symphony.oss.models.internal.pod.canon.IMessageEnvelope;
import com.symphony.oss.models.internal.pod.canon.IPodInfo;
import com.symphony.oss.models.internal.pod.canon.IThreadOfMessages;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;
import com.symphony.oss.models.internal.pod.canon.PodInternalModel;
import com.symphony.oss.models.internal.pod.canon.facade.IAccountInfo;
import com.symphony.oss.models.object.canon.EncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.EncryptedApplicationPayloadAndHeader;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayloadAndHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
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
public class AllegroApi extends AllegroBaseApi implements IAllegroApi
{
  private static final Logger                   log_                       = LoggerFactory.getLogger(AllegroApi.class);

  private static final String                   FORMAT_MESSAGEMLV2         = "com.symphony.messageml.v2";
  private static final ObjectMapper             OBJECT_MAPPER              = new ObjectMapper();  // TODO: get rid of this
  private static final int                      ENCRYPTION_ORDINAL         = 0;
  private static final int                      MEIDA_ENCRYPTION_ORDINAL   = 2;
  
  private static final ObjectMapper  AUTO_CLOSE_MAPPER = new ObjectMapper().configure(Feature.AUTO_CLOSE_SOURCE, false);

  private final PodAndUserId                    userId_;
  private final String                          userName_;
  private final String                          clientType_;
  private final ICipherSuite                    cipherSuite_;
  private final AuthHandler                     authHandler_;
  private final PodHttpModelClient              podApiClient_;
  private final PodInternalHttpModelClient      podInternalApiClient_;
  private final KmInternalHttpModelClient       kmInternalClient_;
  private final IAllegroCryptoClient            cryptoClient_;
  private final IPodInfo                        podInfo_;
  private final AllegroDataProvider             dataProvider_;
  private final V4MessageTransformer            messageTramnsformer_;
  private final EncryptionHandler               agentEncryptionHandler_;

  private PodAndUserId                          internalUserId_;
  private PodId                                 podId_;

  private final Supplier<IAccountInfo>          accountInfoProvider_;
  private final Supplier<X509Certificate>       podCertProvider_;

  private LiveCurrentMessageFactory             liveCurrentMessageFactory_ = new LiveCurrentMessageFactory();

  
  private AllegroDatafeedClient datafeedClient_;

  private ServiceTokenManager serviceTokenManager_;
  
  /**
   * Constructor.
   * 
   * @param builder The builder containing all initialisation values.
   */
  AllegroApi(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    log_.info("AllegroApi constructor start");
    
    userName_ = builder.userName_;
    
    modelRegistry_
        .withFactories(CryptoModel.FACTORIES)
        .withFactories(ChatModel.FACTORIES)
        .withFactories(PodModel.FACTORIES)
        .withFactories(PodInternalModel.FACTORIES)
        .withFactories(KmInternalModel.FACTORIES)
        ;
    
    clientType_     = getClientVersion();
    cipherSuite_    = builder.cipherSuite_;
    

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

    cryptoClient_ = new AllegroCryptoClient(httpClient_, podInternalApiClient_, kmInternalClient_,
        podInfo_, internalUserId_,
        accountInfoProvider_,
        modelRegistry_);
    
    messageTramnsformer_= new V4MessageTransformer(clientType_);
    
    agentEncryptionHandler_ = new EncryptionHandler(cryptoClient_);
    
    log_.info("userId_ = " + userId_);
    
    serviceTokenManager_ = new ServiceTokenManager(podInternalApiClient_, httpClient_);
    
    datafeedClient_ = new AllegroDatafeedClient(serviceTokenManager_, modelRegistry_, httpClient_, builder.podUrl_);
    
    log_.info("allegroApi constructor done.");
  }

  @Override
  public PodAndUserId getUserId()
  {
    return userId_;
  }
  
  /**
   * Convert the given internal or external user ID into an external user ID.
   * 
   * @param internalOrExternalUserId  An internal or external user ID
   * 
   * @return The external user ID for the given user ID.
   */
  private PodAndUserId toExternalUserId(PodAndUserId internalOrExternalUserId)
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

  private IPodInfo getPodInfo()
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
          request.getConsumerManager().getUnprocessableMessageConsumer().consume(lcmessage, trace, "Failed to process message", e);
        }
      }
    }
  }
  
  @Override
  public FeedId createMessageFeed()
  {
    return datafeedClient_.createFeed();
  }
  
  @Override
  public List<FeedId> listMessageFeeds()
  {
    return datafeedClient_.listFeeds();
  }
  
  @Override
  public AckId fetchFeedMessages(FetchFeedMessagesRequest request)
  {
    try(ITraceContextTransaction traceTransaction = traceContextFactory_.createTransaction("FetchFeedMessagesRequest", request.getFeedId().toString()))
    {
      ITraceContext trace = traceTransaction.open();
      
      return datafeedClient_.fetchFeedEvents(request.getFeedId(), request.getAckId(), request.getConsumerManager(), this, trace);
    }
  }
  
  @Override
  public EncryptedApplicationPayloadBuilder newEncryptedApplicationPayloadBuilder()
  {
    return new EncryptedApplicationPayloadBuilder();
  }
  
  @Override
  public EncryptedApplicationPayloadAndHeaderBuilder newEncryptedApplicationPayloadAndHeaderBuilder()
  {
    return new EncryptedApplicationPayloadAndHeaderBuilder();
  }

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
  abstract class EncryptablePayloadbuilder<T extends EncryptablePayloadbuilder<T,B>, B extends IEntity> extends EntityBuilder<T, B>
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
    
    BaseEncryptedApplicationPayloadBuilder(Class<T> type, P builder)
    {
      super(type);
      builder_ = builder;
    }

    @Override
    public ThreadId getThreadId()
    {
      return builder_.getThreadId();
    }

    @Override
    T withEncryptedPayload(
        EncryptedData value)
    {
      builder_.withEncryptedPayload(value);
      
      return self();
    }

    @Override
    T withCipherSuiteId(
        CipherSuiteId value)
    {
      builder_.withCipherSuiteId(value);
      
      return self();
    }

    @Override
    T withRotationId(RotationId value)
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
  
  /**
   * Builder for an EncryptedApplicationPayload.
   * 
   * @author Bruce Skingle
   *
   */
  public class EncryptedApplicationPayloadBuilder extends BaseEncryptedApplicationPayloadBuilder<EncryptedApplicationPayloadBuilder, IEncryptedApplicationPayload, EncryptedApplicationPayload.Builder>
  {
    EncryptedApplicationPayloadBuilder()
    {
      super(EncryptedApplicationPayloadBuilder.class, new EncryptedApplicationPayload.Builder());
    }

    /**
     * Set the id of the thread with whose content key this object will be encrypted.
     * 
     * @param threadId The id of the thread with whose content key this object will be encrypted.
     * 
     * @return This (fluent method).
     */
    public EncryptedApplicationPayloadBuilder withThreadId(ThreadId threadId)
    {
      builder_.withThreadId(threadId);
      
      return self();
    }

    @Override
    protected IEncryptedApplicationPayload construct()
    {
      return builder_.build();
    }
  }
  
  /**
   * Builder for an EncryptedApplicationPayload plus header.
   * 
   * @author Bruce Skingle
   *
   */
  public class EncryptedApplicationPayloadAndHeaderBuilder extends BaseEncryptedApplicationPayloadBuilder<EncryptedApplicationPayloadAndHeaderBuilder, IEncryptedApplicationPayloadAndHeader, EncryptedApplicationPayloadAndHeader.Builder>
  {
    EncryptedApplicationPayloadAndHeaderBuilder()
    {
      super(EncryptedApplicationPayloadAndHeaderBuilder.class, new EncryptedApplicationPayloadAndHeader.Builder());
    }

    /**
     * Set the id of the thread with whose content key this object will be encrypted.
     * 
     * @param threadId The id of the thread with whose content key this object will be encrypted.
     * 
     * @return This (fluent method).
     */
    public EncryptedApplicationPayloadAndHeaderBuilder withThreadId(ThreadId threadId)
    {
      builder_.withThreadId(threadId);
      
      return self();
    }

    /**
     * Set the unencrypted header for this object.
     * 
     * @param header The unencrypted header for this object.
     * 
     * @return This (fluent method).
     */
    public EncryptedApplicationPayloadAndHeaderBuilder withHeader(IApplicationObjectHeader header)
    {
      builder_.withHeader(header);
      
      return self();
    }

    @Override
    protected IEncryptedApplicationPayloadAndHeader construct()
    {
      return builder_.build();
    }
  }
  
  /**
   * Super class for AppplicationObject builder and updater.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The concrete type for fluent methods.
   */
  abstract class BaseApplicationObjectBuilder<T extends BaseApplicationObjectBuilder<T>> extends EncryptablePayloadbuilder<T, IStoredApplicationObject>
  {
    protected final StoredApplicationObject.Builder  builder_ = new StoredApplicationObject.Builder();
    
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

    @Override
    public ThreadId getThreadId()
    {
      return builder_.getThreadId();
    }

    @Override
    T withEncryptedPayload(
        EncryptedData value)
    {
      builder_.withEncryptedPayload(value);
      
      return self();
    }

    @Override
    T withCipherSuiteId(
        CipherSuiteId value)
    {
      builder_.withCipherSuiteId(value);
      
      return self();
    }

    @Override
    T withRotationId(RotationId value)
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

      builder_.withOwner(getUserId());
      
      if(getThreadId() == null)
      {
        if(payload_ != null || builder_.getEncryptedPayload() != null)
          throw new IllegalStateException("ThreadId is required unless there is no payload.");
      }
      else
      {
        if(payload_ == null)
        {
          if(builder_.getEncryptedPayload() == null)
            throw new IllegalStateException("One of Payload or EncryptedPayload is required.");
        }
        else
        {
          cryptoClient_.encrypt(this);
        }
      }
      
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
    
//    /**
//     * Set the already encrypted object payload and header.
//     * 
//     * @param payload The encrypted object payload and header.
//     * 
//     * @return This (fluent method).
//     */
//    public ApplicationObjectBuilder withEncryptedPayloadAndHeader(IEncryptedApplicationPayloadAndHeader payload)
//    {
//      withHeader(payload.getHeader());
//
//      return withEncryptedPayload(payload);
//    }
//    
//    /**
//     * Set the already encrypted object payload.
//     * 
//     * @param payload The encrypted object payload.
//     * 
//     * @return This (fluent method).
//     */
//    public ApplicationObjectBuilder withEncryptedPayload(IEncryptedApplicationPayload payload)
//    {
//      withEncryptedPayload(payload.getEncryptedPayload());
//      withRotationId(payload.getRotationId());
//      withCipherSuiteId(payload.getCipherSuiteId());
//      withThreadId(payload.getThreadId());
//      
//      return self();
//    }
    
    @Override
    protected IStoredApplicationObject construct()
    {
      return builder_.build();
    }
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
    
    /**
     * Set the already encrypted object payload and header.
     * 
     * @param payload The encrypted object payload and header.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectUpdater withEncryptedPayloadAndHeader(IEncryptedApplicationPayloadAndHeader payload)
    {
      withHeader(payload.getHeader());

      return withEncryptedPayload(payload);
    }
    
    /**
     * Set the already encrypted object payload.
     * 
     * @param payload The encrypted object payload.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectUpdater withEncryptedPayload(IEncryptedApplicationPayload payload)
    {
      if(!builder_.getThreadId().equals(payload.getThreadId()))
        throw new IllegalArgumentException("The threadId of an object cannot be changed. The object being updated has thread ID " + builder_.getThreadId());
      
      withEncryptedPayload(payload.getEncryptedPayload());
      withRotationId(payload.getRotationId());
      withCipherSuiteId(payload.getCipherSuiteId());
      
      return self();
    }

    @Override
    protected IStoredApplicationObject construct()
    {
      return builder_.build();
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
  public IApplicationObjectPayload decryptObject(IStoredApplicationObject storedApplicationObject)
  {
    if(storedApplicationObject.getEncryptedPayload() == null)
      return null;
    
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
  
  private IReceivedMaestroMessage buildMaestroMessage(IMaestroMessage message)
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
            
        if(context.getEntityJson().isObject())
        {
          ImmutableJsonObject obj = (ImmutableJsonObject)JacksonAdaptor.adapt(context.getEntityJson()).immutify();
          
          builder.withEntityJson(new EntityJson(obj, modelRegistry_));
        }
        else if(context.getEntityJson().isTextual())
        {
            String      encryptedEntityJson = context.getEntityJson().asText();
            
            if(encryptedEntityJson != null)
            {
              String jsonString = cryptoClient_.decrypt(message.getThreadId(), encryptedEntityJson);
              
              builder.withEntityJson(new EntityJson(parseOneJsonObject(jsonString), modelRegistry_));
            }
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
  void consume(AbstractConsumerManager consumerManager, Object payload, ITraceContext trace) throws RetryableConsumerException, FatalConsumerException
  {
    consumerManager.consume(payload, trace, this);
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
  static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends IAllegroApi>
  extends AllegroBaseApi.AbstractBuilder<T, B>
  {
    protected URL                           podUrl_;
    protected String                        userName_;

    public AbstractBuilder(Class<T> type)
    {
      super(type);
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

    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkNotNull(podUrl_, "Pod URL");
      faultAccumulator.checkNotNull(userName_, "User Name");
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
