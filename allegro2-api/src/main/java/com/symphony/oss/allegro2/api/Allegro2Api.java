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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.canon.runtime.IEntityFactory;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.exception.BadRequestException;
import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.canon.runtime.http.client.IResponseHandler;
import com.symphony.oss.canon.runtime.http.client.IResponseHandlerContext;
import com.symphony.oss.canon.runtime.http.client.ResponseHandlerAction;
import com.symphony.oss.commons.dom.json.IImmutableJsonDomNode;
import com.symphony.oss.commons.dom.json.IJsonDomNode;
import com.symphony.oss.commons.dom.json.ImmutableJsonObject;
import com.symphony.oss.commons.dom.json.MutableJsonObject;
import com.symphony.oss.commons.dom.json.jackson.JacksonAdaptor;
import com.symphony.oss.commons.fault.CodingFault;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.commons.immutable.ImmutableByteArray;
import com.symphony.oss.model.chat.LiveCurrentMessageFactory;
import com.symphony.oss.models.allegro.canon.AllegroModel;
import com.symphony.oss.models.allegro.canon.EntityJson;
import com.symphony.oss.models.allegro.canon.facade.Allegro2Configuration;
import com.symphony.oss.models.allegro.canon.facade.ChatMessage;
import com.symphony.oss.models.allegro.canon.facade.ConnectionSettings;
import com.symphony.oss.models.allegro.canon.facade.IAllegro2Configuration;
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
import com.symphony.oss.models.core.canon.ApplicationPayload;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.core.canon.facade.ApplicationRecord;
import com.symphony.oss.models.core.canon.facade.IApplicationRecord;
import com.symphony.oss.models.core.canon.facade.IEncryptedApplicationRecord;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.core.canon.facade.UserId;
import com.symphony.oss.models.crypto.canon.CryptoModel;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.crypto.canon.PemPrivateKey;
import com.symphony.oss.models.crypto.cipher.CipherSuite;
import com.symphony.oss.models.crypto.cipher.CipherSuiteUtils;
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
import com.symphony.oss.models.pod.canon.IPodCertificate;
import com.symphony.oss.models.pod.canon.IStreamAttributes;
import com.symphony.oss.models.pod.canon.IStreamType;
import com.symphony.oss.models.pod.canon.IUserV2;
import com.symphony.oss.models.pod.canon.IV2UserList;
import com.symphony.oss.models.pod.canon.PodHttpModelClient;
import com.symphony.oss.models.pod.canon.PodModel;
import com.symphony.oss.models.pod.canon.StreamFilter;
import com.symphony.oss.models.pod.canon.StreamType;
import com.symphony.oss.models.pod.canon.StreamTypeEnum;
import com.symphony.s2.authc.canon.AuthcModel;

public class Allegro2Api implements IAllegro2Api
{

  private static final String                   FORMAT_MESSAGEMLV2         = "com.symphony.messageml.v2";
  private static final ObjectMapper             OBJECT_MAPPER              = new ObjectMapper();  // TODO: get rid of this
  private static final int                      ENCRYPTION_ORDINAL         = 0;
  private static final int                      MEIDA_ENCRYPTION_ORDINAL   = 2;
  
  private static final ObjectMapper             AUTO_CLOSE_MAPPER          = new ObjectMapper().configure(Feature.AUTO_CLOSE_SOURCE, false);
  private static final Logger                   log_                       = LoggerFactory.getLogger(Allegro2Api.class);

  private final ModelRegistry                   modelRegistry_;
  final AllegroCryptoClient                     cryptoClient_;
  private final PodAndUserId                    userId_;
  private final String                          userName_;
  private final String                          clientType_;
  private final ICipherSuite                    cipherSuite_;
  private final IAuthHandler                    authHandler_;
  private final PodHttpModelClient              podApiClient_;
  private final PodInternalHttpModelClient      podInternalApiClient_;
  private final KmInternalHttpModelClient       kmInternalClient_;
  private final AllegroDatafeedClient           datafeedClient_;
  private final IPodInfo                        podInfo_;
  private final AllegroDataProvider             dataProvider_;
  private final V4MessageTransformer            messageTramnsformer_;
  private final EncryptionHandler               agentEncryptionHandler_;
  private final CloseableHttpClient             podHttpClient_;
  private final CloseableHttpClient             keyManagerHttpClient_;

  private PodAndUserId                          internalUserId_;
  private PodId                                 podId_;

  private final Supplier<IAccountInfo>          accountInfoProvider_;
  private final Supplier<X509Certificate>       podCertProvider_;

  private LiveCurrentMessageFactory             liveCurrentMessageFactory_ = new LiveCurrentMessageFactory();
  private ServiceTokenManager                   serviceTokenManager_;

  private Map<Integer, IResponseHandler> responseHandlerMap_ = new HashMap<>();



  protected Allegro2Api(AbstractBuilder<?, ?> builder)
  {
    log_.info("Allegro2Api constructor start with config " + builder.config_.getRedacted());
    
    modelRegistry_ = new ModelRegistry()
//        .withFactories(ObjectModel.FACTORIES)
        .withFactories(AuthcModel.FACTORIES)
//        .withFactories(AuthzModel.FACTORIES)
        .withFactories(CoreModel.FACTORIES)
        ;
    
    for(IEntityFactory<?, ?, ?> factory : builder.factories_)
      modelRegistry_.withFactories(factory);
    
    podHttpClient_        = builder.getPodHttpClient();
    keyManagerHttpClient_ = builder.getKeyManagerHttpClient();
    
    userName_ = builder.config_.getUserName();
    
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
        builder.config_.getPodUrl(), "/pod", null, responseHandlerMap_);
    
    authHandler_    = createAuthHandler(builder); 
    
    log_.info("sbe auth....");
    authHandler_.authenticate(true, false);
    
    responseHandlerMap_.put(401, new AuthResponseHandler());
    
    log_.info("fetch podInfo_....");
    podInternalApiClient_ = new PodInternalHttpModelClient(
        modelRegistry_,
        builder.config_.getPodUrl(), null, null, responseHandlerMap_);
    
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
              .execute(podHttpClient_);
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
              .execute(podHttpClient_);
            
          log_.info("fetch podCert....got " + podCert.getCertificate());
          value_ = cipherSuite_.certificateFromPem(podCert.getCertificate());
        }
        return value_;
      }
    };

    podInfo_ = getPodInfo();
    
    log_.info("keymanager auth....");
    podId_ = PodId.newBuilder().build(podInfo_.getExternalPodId());
    authHandler_.setKeyManagerUrl(podInfo_.getKeyManagerUrl());
    authHandler_.authenticate(false, true);
    
    
    log_.info("getAccountInfo....");
    IAccountInfo accountInfo = accountInfoProvider_.get();
    
    internalUserId_ = PodAndUserId.newBuilder().build(accountInfo.getUserName());
    userId_ = toExternalUserId(internalUserId_);
    
    kmInternalClient_ = new KmInternalHttpModelClient(
        modelRegistry_,
        podInfo_.getKeyManagerUrl(), null, null, responseHandlerMap_);
    
    dataProvider_ = new AllegroDataProvider(podHttpClient_, podApiClient_, podInfo_, authHandler_.getSessionToken());

    cryptoClient_ = new AllegroCryptoClient(podHttpClient_, podInternalApiClient_,
        keyManagerHttpClient_, kmInternalClient_,
        podInfo_, internalUserId_,
        accountInfoProvider_,
        modelRegistry_);
    
    messageTramnsformer_= new V4MessageTransformer(clientType_);
    
    agentEncryptionHandler_ = new EncryptionHandler(cryptoClient_);
    
    log_.info("userId_ = " + userId_);
    
    serviceTokenManager_ = new ServiceTokenManager(podInternalApiClient_, podHttpClient_, authHandler_);
    
    datafeedClient_ = new AllegroDatafeedClient(serviceTokenManager_, modelRegistry_, podHttpClient_, builder.config_.getPodUrl(), responseHandlerMap_);
    
    log_.info("AllegroPodApi constructor done.");
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
  static public abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends IAllegro2Api>
  extends BaseAbstractBuilder<T, B>
  {

    ICipherSuite                    cipherSuite_;
    Supplier<String>                sessionTokenSupplier_;
    Supplier<String>                keyManagerTokenSupplier_;

    CloseableHttpClient             podHttpClient_;
    CloseableHttpClient             keyManagerHttpClient_;
    CloseableHttpClient             certSessionAuthHttpClient_;
    CloseableHttpClient             certKeyAuthHttpClient_;
    CloseableHttpClient             defaultCertAuthHttpClient_;

    IAllegro2Configuration        config_;
    CloseableHttpClient             defaultHttpClient_;
    PrivateKey                      rsaCredential_;
    boolean                         rsaCredentialIsSet_;
    CookieStore                     cookieStore_          = new BasicCookieStore();
    List<IEntityFactory<?, ?, ?>>   factories_            = new LinkedList<>();
    ModelRegistry                   allegroModelRegistry_ = new ModelRegistry()
        .withFactories(AllegroModel.FACTORIES)
        .withFactories(AuthcModel.FACTORIES);
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    public T withRsaCredential(PrivateKey rsaCredential)
    {
      rsaCredential_ = rsaCredential;
      rsaCredentialIsSet_ = true;
      
      return self();
    }
    
    public T withConfiguration(IAllegro2Configuration configuration)
    {
      config_ = configuration;
      
      return self();
    }
    
    public T withConfiguration(Reader reader)
    {
      return withConfiguration(allegroModelRegistry_.parseOne(reader, Allegro2Configuration.TYPE_ID, IAllegro2Configuration.class));
    }
    
    public T withConfigurationFile(String fileName) throws FileNotFoundException, IOException
    {
      try(Reader reader = new FileReader(fileName))
      {
        return withConfiguration(reader);
      }
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
    
    public T withSessionTokenSupplier(Supplier<String> sessionTokenSupplier)
    {
      sessionTokenSupplier_ = sessionTokenSupplier;
      
      return self();
    }
    
    public T withKeymanagerTokenSupplier(Supplier<String> keymanagerTokenSupplier)
    {
      keyManagerTokenSupplier_ = keymanagerTokenSupplier;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(config_, "Configuration");
      
      // AllegroApi sets this when we are called from there
      if(!rsaCredentialIsSet_)
      {
        if(config_.getRsaPemCredential() == null)
        {
          if(config_.getRsaPemCredentialFile() == null)
          {
            rsaCredential_ = null;
          }
          else
          {
            File file = new File(config_.getRsaPemCredentialFile());
            
            if(!file.canRead())
              throw new IllegalArgumentException("Credential file \"" + file.getAbsolutePath() + "\" is unreadable");
            
            try
            {
              PemPrivateKey pemPrivateKey = PemPrivateKey.newBuilder().build(new String(Files.toByteArray(file)));
              rsaCredential_ = CipherSuiteUtils.privateKeyFromPem(pemPrivateKey);
            }
            catch (IOException e)
            {
              throw new IllegalArgumentException("Unable to read credential file \""  + file.getAbsolutePath() + "\".", e);
            }
          }
        }
        else
        {
          rsaCredential_ = CipherSuiteUtils.privateKeyFromPem(config_.getRsaPemCredential());
        }
      }
      
      cipherSuite_ = config_.getCipherSuiteId() == null ? CipherSuite.getDefault() : CipherSuite.get(config_.getCipherSuiteId());
      
      if(sessionTokenSupplier_ != null || keyManagerTokenSupplier_!= null )
      {
        if(sessionTokenSupplier_ == null || keyManagerTokenSupplier_== null )
          faultAccumulator.error("SessionToekn and KeymanagerToken must be specified together");
      }
      else
      {

        if(config_.getAuthCertFile() != null)
        {
          faultAccumulator.checkNotNull(config_.getAuthCertFilePassword(), "With Auth Cert File, Cert File Password");
          faultAccumulator.checkNotNull(config_.getSessionAuthUrl(), "With Auth Cert File, Session Auth URL");
          faultAccumulator.checkNotNull(config_.getKeyAuthUrl(), "With Auth Cert File, Key Auth URL");
        }
        else if(config_.getAuthCert() != null)
        {
          faultAccumulator.checkNotNull(config_.getAuthCertPrivateKey(), "With Auth Cert, Auth Cert Private Key");
          faultAccumulator.checkNotNull(config_.getSessionAuthUrl(), "With Auth Cert, Session Auth URL");
          faultAccumulator.checkNotNull(config_.getKeyAuthUrl(), "With Auth Cert, Key Auth URL");
        }
        else if(rsaCredential_ == null)
        {
          if(rsaCredential_ == null)
            faultAccumulator.error("rsaCredential is required");
          
          faultAccumulator.checkNotNull(config_.getUserName(), "With RSA Credential, User Name");
        }
        
        
      }
      faultAccumulator.checkNotNull(config_.getPodUrl(), "Pod URL");
    }
    
    synchronized CloseableHttpClient getDefaultHttpClient()
    {
      if(defaultHttpClient_ == null)
      {
        if(config_.getDefaultConnectionSettings() == null)
        {
          defaultHttpClient_ = new ConnectionSettings.Builder().build().createHttpClient(cookieStore_);
        }
        else
        {
          defaultHttpClient_ = config_.getDefaultConnectionSettings().createHttpClient(cookieStore_);
        }
      }
      
      return defaultHttpClient_;
    }

    public synchronized CloseableHttpClient getPodHttpClient()
    {
      if(podHttpClient_ == null)
      {
        if(config_.getPodConnectionSettings() == null)
        {
          podHttpClient_ = getDefaultHttpClient();
        }
        else
        {
          podHttpClient_ = config_.getPodConnectionSettings().createHttpClient(cookieStore_);
        }
      }
      
      return podHttpClient_;
    }
    
    public synchronized CloseableHttpClient getKeyManagerHttpClient()
    {
      if(keyManagerHttpClient_ == null)
      {
        if(config_.getKeyManagerConnectionSettings() == null)
        {
          keyManagerHttpClient_ = getDefaultHttpClient();
        }
        else
        {
          keyManagerHttpClient_ = config_.getKeyManagerConnectionSettings().createHttpClient(cookieStore_);
        }
      }
      
      return keyManagerHttpClient_;
    }
    
    protected synchronized CloseableHttpClient getDefaultCertAuthHttpClient(SSLContextBuilder sslContextBuilder)
    {
      if(defaultCertAuthHttpClient_ == null)
      {
        if(config_.getDefaultConnectionSettings() == null)
        {
          defaultCertAuthHttpClient_ = new ConnectionSettings.Builder().build().createHttpClient(cookieStore_, sslContextBuilder);
        }
        else
        {
          defaultCertAuthHttpClient_ = config_.getDefaultConnectionSettings().createHttpClient(cookieStore_, sslContextBuilder);
        }
      }
      
      return defaultCertAuthHttpClient_;
    }
    
    public synchronized CloseableHttpClient getCertSessionAuthHttpClient(SSLContextBuilder sslContextBuilder)
    {
      if(certSessionAuthHttpClient_ == null)
      {
        if(config_.getCertSessionAuthConnectionSettings() == null)
        {
          certSessionAuthHttpClient_ = getDefaultCertAuthHttpClient(sslContextBuilder);
        }
        else
        {
          certSessionAuthHttpClient_ = config_.getCertSessionAuthConnectionSettings().createHttpClient(cookieStore_, sslContextBuilder);
        }
      }
      
      return certSessionAuthHttpClient_;
    }
    
    public synchronized CloseableHttpClient getCertKeyAuthHttpClient(SSLContextBuilder sslContextBuilder)
    {
      if(certKeyAuthHttpClient_ == null)
      {
        if(config_.getCertKeyAuthConnectionSettings() == null)
        {
          certKeyAuthHttpClient_ = getDefaultCertAuthHttpClient(sslContextBuilder);
        }
        else
        {
          certKeyAuthHttpClient_ = config_.getCertKeyAuthConnectionSettings().createHttpClient(cookieStore_, sslContextBuilder);
        }
      }
      
      return certKeyAuthHttpClient_;
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, IAllegro2Api>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected IAllegro2Api construct()
    {
      return new Allegro2Api(this);
    }
  }

  @Override
  public ModelRegistry getModelRegistry()
  {
    return modelRegistry_;
  }

//  @Override
//  public IApplicationObjectPayload decryptObject(IEncryptedApplicationPayload storedApplicationObject)
//  {
//    if(storedApplicationObject.getEncryptedPayload() == null)
//      return null;
//    
//    return cryptoClient_.decrypt(storedApplicationObject);
//  }
//
//  @Override
//  public <T extends IApplicationObjectPayload> T decryptObject(IEncryptedApplicationPayload storedApplicationObject,
//      Class<T> type)
//  {
//    if(storedApplicationObject.getEncryptedPayload() == null)
//      return null;
//    
//    IApplicationObjectPayload payload = cryptoClient_.decrypt(storedApplicationObject);
//    
//    if(type.isInstance(payload))
//      return type.cast(payload);
//    
//    throw new IllegalStateException("Retrieved object is of type " + payload.getClass() + " not " + type);
//  }
  


  private class AuthResponseHandler implements IResponseHandler
  {
    @Override
    public IResponseHandlerContext prepare()
    {
      return new AuthResponseHandlerContext();
    }
  }
  
  private class AuthResponseHandlerContext implements IResponseHandlerContext
  {
    String sessionToken = authHandler_.getSessionToken();
    
    @Override
    public ResponseHandlerAction handle(CloseableHttpResponse response)
    {
      return authHandler_.reauthenticate(sessionToken);
    }
    
  }

  private IAuthHandler createAuthHandler(AbstractBuilder<?, ?> builder)
  {

    if(builder.sessionTokenSupplier_ != null)
      return new DummyAuthHandler(builder.sessionTokenSupplier_, builder.keyManagerTokenSupplier_, builder.cookieStore_, builder.config_.getPodUrl());
    
    if(builder.config_.getAuthCertFile() != null || builder.config_.getAuthCert() != null)
      return new CertAuthHandler(builder);
    
    return new AuthHandler(builder, userName_);
  }
  
  @Override
  public void close()
  {
    try
    {
      podHttpClient_.close();
      keyManagerHttpClient_.close();
      authHandler_.close();
    }
    catch (IOException e)
    {
      log_.error("Unable to close HttpClient", e);
    }
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
  public String getApiAuthorizationToken()
  {
    return serviceTokenManager_.getCommonJwt();
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
  public IUserV2 fetchUserByName(String userName) throws NotFoundException
  {
    if(userName.indexOf(',') != -1)
      throw new BadRequestException("Only a single userName may be passed, try getUsersByName() instead.");
    
    IV2UserList result = podApiClient_.newV3UsersGetHttpRequestBuilder()
        .withSessionToken(authHandler_.getSessionToken())
        .withUsername(userName)
        .withLocal(true)
        .build()
        .execute(podHttpClient_);
    
    if(result.getUsers().size()==1)
      return result.getUsers().get(0);
    
    throw new NotFoundException("User not found " + result.getErrors());
  }

  @Override
  public IV2UserList fetchUsersByName(String ...userNames)
  {
    String userNamesList = String.join(",", userNames);
    
    return podApiClient_.newV3UsersGetHttpRequestBuilder()
        .withSessionToken(authHandler_.getSessionToken())
        .withUsername(userNamesList)
        .withLocal(true)
        .build()
        .execute(podHttpClient_);
  }
  


  @Override
  public IUserV2 fetchUserById(PodAndUserId userId) throws NotFoundException
  {
    IV2UserList result = podApiClient_.newV3UsersGetHttpRequestBuilder()
        .withSessionToken(authHandler_.getSessionToken())
        .withUid(userId)
        .withLocal(true)
        .build()
        .execute(podHttpClient_);
    
    if(result.getUsers().size()==1)
      return result.getUsers().get(0);
    
    throw new NotFoundException("User not found " + result.getErrors());
  }
  
  @Override
  public IUserV2 getUserInfo()
  {
    return podApiClient_.newV2UserGetHttpRequestBuilder()
        .withSessionToken(authHandler_.getSessionToken())
        .withUid(userId_)
        .build()
        .execute(podHttpClient_);
  }

  private IPodInfo getPodInfo()
  {
    return podInternalApiClient_.newWebcontrollerPublicPodInfoGetHttpRequestBuilder()
        .build()
        .execute(podHttpClient_)
        .getData();
  }

  @Override
  public IUserV2 getSessioninfo()
  {
    return podApiClient_.newV2SessioninfoGetHttpRequestBuilder()
        .withSessionToken(authHandler_.getSessionToken())
        .build()
        .execute(podHttpClient_);
  }

  @Override
  public String getMessage(String messageId)
  {
    String urlSafeMessageId = Base64.encodeBase64URLSafeString(Base64.decodeBase64(messageId));
    
    ISocialMessage encryptedMessage = podInternalApiClient_.newWebcontrollerDataqueryRetrieveMessagePayloadGetHttpRequestBuilder()
      .withMessageId(urlSafeMessageId)
      .build()
      .execute(podHttpClient_);
    
    return encryptedMessage.toString();
  }

  @Override
  public void fetchRecentMessagesFromPod(FetchRecentMessagesRequest request)
  {
    IThreadOfMessages thread = podInternalApiClient_.newDataqueryApiV3MessagesThreadGetHttpRequestBuilder()
        .withId(request.getThreadId().toBase64UrlSafeString())
        .withFrom(0L)
        .withLimit(request.getMaxItems())
        .withExcludeFields("tokenIds")
        .build()
        .execute(podHttpClient_);
      
    for(IMessageEnvelope envelope : thread.getEnvelopes())
    {
      ILiveCurrentMessage lcmessage = liveCurrentMessageFactory_.newLiveCurrentMessage(envelope.getMessage().getJsonObject().mutify(), modelRegistry_);
      
      request.getConsumerManager().accept(lcmessage);
    }
  }

  @Override
  public List<IStreamAttributes> fetchStreams(FetchStreamsRequest fetchStreamsRequest)
  {
    StreamFilter.Builder builder = new StreamFilter.Builder()
      .withIncludeInactiveStreams(fetchStreamsRequest.isInactive())
      ;
    
    if(!fetchStreamsRequest.getStreamTypes().isEmpty())
    {
      List<IStreamType> streamTypes = new LinkedList<>();
      
      for(StreamTypeEnum type : fetchStreamsRequest.getStreamTypes())
        streamTypes.add(new StreamType.Builder().withType(type).build());
      
      builder.withStreamTypes(streamTypes);
    }
    
    
    List<IStreamAttributes> streams = podApiClient_.newV1StreamsListPostHttpRequestBuilder()
        .withSessionToken(authHandler_.getSessionToken())
        .withSkip(fetchStreamsRequest.getSkip())
        .withLimit(fetchStreamsRequest.getLimit())
        .withCanonPayload(builder.build())
        .build()
        .execute(podHttpClient_);

    
    return streams;
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
    return datafeedClient_.fetchFeedEvents(request.getFeedId(), request.getAckId(), request.getConsumerManager());
  }
  
  @Override
  public ApplicationRecordBuilder newApplicationRecordBuilder()
  {
    return new ApplicationRecordBuilder(this, modelRegistry_);
  }
  
  @Override
  public ChatMessage.Builder newChatMessageBuilder()
  {
    return new ChatMessage.Builder().withRegistry(modelRegistry_, dataProvider_);
  }
  
  @Override
  public AllegroConsumerManager.AbstractBuilder<?,?> newConsumerManagerBuilder()
  {
    return new AllegroConsumerManager.Builder(this, getModelRegistry());
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
        .execute(podHttpClient_);
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
  public void encrypt(EncryptablePayloadBuilder<?, ?> builder)
  {
    cryptoClient_.encrypt(builder);
  }

  @Override
  public ImmutableByteArray decrypt(ThreadId threadId, RotationId rotationId, EncryptedData encryptedPayload)
  {
    return cryptoClient_.decrypt(threadId, rotationId, encryptedPayload);
  }
  
  @Override
  public IApplicationRecord decryptObject(IEncryptedApplicationRecord encryptedApplicationRecord)
  {
    ApplicationRecord.Builder builder = new ApplicationRecord.Builder()
        .withHeader(encryptedApplicationRecord.getHeader());
    
    if(encryptedApplicationRecord.getEncryptedPayload() != null)
    {
      ImmutableByteArray plainText = decrypt(encryptedApplicationRecord.getThreadId(), encryptedApplicationRecord.getRotationId(), 
          encryptedApplicationRecord.getEncryptedPayload());
      
      IEntity entity = modelRegistry_.parseOne(plainText.getReader());
      
      if(entity instanceof IApplicationPayload)
      {
        builder.withPayload((IApplicationPayload)entity);
      }
      else
      {
        builder.withPayload(new ApplicationPayload(entity.getJsonObject(), modelRegistry_));
      }
    }
    
    return builder.build();
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
}
