/*
 *
 *
 * Copyright 2020 Symphony Communication Services, LLC.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.canon.runtime.EntityBuilder;
import org.symphonyoss.s2.canon.runtime.IEntityFactory;
import org.symphonyoss.s2.canon.runtime.ModelRegistry;
import org.symphonyoss.s2.canon.runtime.exception.BadRequestException;
import org.symphonyoss.s2.canon.runtime.exception.NotFoundException;
import org.symphonyoss.s2.canon.runtime.exception.ServerErrorException;
import org.symphonyoss.s2.canon.runtime.http.IRequestAuthenticator;
import org.symphonyoss.s2.canon.runtime.http.client.IAuthenticationProvider;
import org.symphonyoss.s2.canon.runtime.jjwt.JwtBase;
import org.symphonyoss.s2.common.dom.json.ImmutableJsonObject;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.core.trace.NoOpContextFactory;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

import com.google.common.io.Files;
import com.symphony.oss.allegro.api.request.FeedQuery;
import com.symphony.oss.allegro.api.request.FetchEntitlementRequest;
import com.symphony.oss.allegro.api.request.FetchFeedObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchObjectVersionsRequest;
import com.symphony.oss.allegro.api.request.FetchPartitionObjectsRequest;
import com.symphony.oss.allegro.api.request.PartitionId;
import com.symphony.oss.allegro.api.request.PartitionQuery;
import com.symphony.oss.allegro.api.request.UpsertFeedRequest;
import com.symphony.oss.allegro.api.request.UpsertPartitionRequest;
import com.symphony.oss.allegro.api.request.VersionQuery;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.core.canon.CoreHttpModelClient;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.core.canon.HashType;
import com.symphony.oss.models.core.canon.ICursors;
import com.symphony.oss.models.core.canon.IPagination;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.PemPrivateKey;
import com.symphony.oss.models.crypto.cipher.CipherSuite;
import com.symphony.oss.models.crypto.cipher.ICipherSuite;
import com.symphony.oss.models.object.canon.DeletionType;
import com.symphony.oss.models.object.canon.FeedRequest;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayloadAndHeader;
import com.symphony.oss.models.object.canon.IFeed;
import com.symphony.oss.models.object.canon.IPageOfAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;
import com.symphony.oss.models.object.canon.IUserPermissionsRequest;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;
import com.symphony.oss.models.object.canon.ObjectModel;
import com.symphony.oss.models.object.canon.ObjectsObjectHashVersionsGetHttpRequestBuilder;
import com.symphony.oss.models.object.canon.PartitionsPartitionHashPageGetHttpRequestBuilder;
import com.symphony.oss.models.object.canon.UserPermissionsRequest;
import com.symphony.oss.models.object.canon.facade.DeletedApplicationObject;
import com.symphony.oss.models.object.canon.facade.FeedObjectDelete;
import com.symphony.oss.models.object.canon.facade.FeedObjectExtend;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IDeletedApplicationObject;
import com.symphony.oss.models.object.canon.facade.IFeedObject;
import com.symphony.oss.models.object.canon.facade.IFeedObjectExtend;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.SortKey;
import com.symphony.oss.models.object.canon.facade.StoredApplicationObject;
import com.symphony.s2.authc.canon.AuthcHttpModelClient;
import com.symphony.s2.authc.canon.AuthcModel;
import com.symphony.s2.authc.canon.IServiceInfo;
import com.symphony.s2.authc.canon.ServiceId;
import com.symphony.s2.authc.model.IAuthcContext;
import com.symphony.s2.authc.model.IMultiTenantService;
import com.symphony.s2.authc.model.MultiTenantService;
import com.symphony.s2.authc.model.RemoteJwtAuthenticator;
import com.symphony.s2.authz.canon.AuthzHttpModelClient;
import com.symphony.s2.authz.canon.AuthzModel;
import com.symphony.s2.authz.canon.EntitlementAction;
import com.symphony.s2.authz.canon.facade.IEntitlement;
import com.symphony.s2.authz.canon.facade.IEntitlementId;
import com.symphony.s2.authz.canon.facade.IPodEntitlementMapping;
import com.symphony.s2.authz.canon.facade.IUserEntitlementMapping;
import com.symphony.s2.authz.canon.facade.PodEntitlementMapping;
import com.symphony.s2.authz.canon.facade.UserEntitlementMapping;
import com.symphony.s2.authz.model.BaseEntitlementValidator;
import com.symphony.s2.authz.model.EntitlementSpecAdaptor;
import com.symphony.s2.authz.model.IEntitlementValidator;
import com.symphony.s2.authz.model.IGeneralEntitlementSpec;
import com.symphony.s2.authz.model.IServiceEntitlementSpecOrIdProvider;

/**
 * Super class of AllegroMultiTenantApi and AllegroApi.
 * 
 * @author Bruce Skingle
 *
 */
abstract class AllegroBaseApi extends AllegroDecryptor implements IAllegroMultiTenantApi
{
  private static final Logger                   log_                       = LoggerFactory.getLogger(AllegroBaseApi.class);
  private static final long                     FAILED_CONSUMER_RETRY_TIME    = TimeUnit.SECONDS.toSeconds(30);
  
  IAuthenticationProvider jwtGenerator_  = new IAuthenticationProvider()
  {
    @Override
    public void authenticate(RequestBuilder builder)
    {
      builder.addHeader(JwtBase.AUTH_HEADER_KEY, JwtBase.AUTH_HEADER_VALUE_PREFIX + getSessionToken());
    }
  };

  final ModelRegistry                   modelRegistry_;
  final CloseableHttpClient             httpClient_;
  final CoreHttpModelClient             coreApiClient_;
  final ObjectHttpModelClient           objectApiClient_;
  final AuthcHttpModelClient            authcApiClient_;
  final AuthzHttpModelClient            authzApiClient_;
  final BaseEntitlementValidator        entitlementValidator_;
  final EntitlementSpecAdaptor          entitlementSpecAdaptor_;
  final ITraceContextTransactionFactory traceFactory_;
  
  private final Map<ServiceId, IServiceInfo>   serviceMap_ = new HashMap<>();
  private RemoteJwtAuthenticator authenticator_;
  
  AllegroBaseApi(AbstractBuilder<?, ?> builder)
  {
    
    traceFactory_ = builder.traceFactory_;
    
    modelRegistry_ = new ModelRegistry()
        .withFactories(ObjectModel.FACTORIES)
        .withFactories(AuthcModel.FACTORIES)
        .withFactories(AuthzModel.FACTORIES)
        .withFactories(CoreModel.FACTORIES)
        ;
    
    for(IEntityFactory<?, ?, ?> factory : builder.factories_)
      modelRegistry_.withFactories(factory);


    httpClient_     = builder.httpclient_;
    
    coreApiClient_  = new CoreHttpModelClient(
        modelRegistry_,
        initUrl(builder.objectStoreUrl_, MultiTenantService.OBJECT), null, jwtGenerator_);
    
    objectApiClient_  = new ObjectHttpModelClient(
        modelRegistry_,
        initUrl(builder.objectStoreUrl_, MultiTenantService.OBJECT), null, jwtGenerator_);
    
    authcApiClient_  = new AuthcHttpModelClient(
        modelRegistry_,
        initUrl(builder.objectStoreUrl_, MultiTenantService.AUTHC), null, jwtGenerator_);
    
    authzApiClient_  = new AuthzHttpModelClient(
        modelRegistry_,
        initUrl(builder.objectStoreUrl_, MultiTenantService.AUTHZ), null, jwtGenerator_);
    
    
//    IMultiTenantServiceRegistry serviceRegistry = new IMultiTenantServiceRegistry()
//        {
//
//          @Override
//          public IServiceInfo fetchServiceInfo(MultiTenantService service)
//          {
//            // TODO Auto-generated method stub
//            return null;
//          }
//      
//        };
    
    entitlementValidator_ = new BaseEntitlementValidator(httpClient_, authzApiClient_, this);
    entitlementSpecAdaptor_ = new EntitlementSpecAdaptor(this);
  }

  private String initUrl(String url, MultiTenantService service)
  {
    if(url.equals("local"))
      return "http://127.0.0.1:" + service.getHttpPort();
    
    return url;
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
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends IAllegroMultiTenantApi>
  extends BaseAbstractBuilder<T, B>
  {
    protected PrivateKey                      rsaCredential_;
    protected PemPrivateKey                   rsaPemCredential_;
    protected CipherSuiteId                   cipherSuiteId_;
    protected ICipherSuite                    cipherSuite_;
    protected CloseableHttpClient             httpclient_;
    protected String                          objectStoreUrl_       = "https://api.symphony.com";
    protected CookieStore                     cookieStore_;
    protected List<IEntityFactory<?, ?, ?>>   factories_            = new LinkedList<>();
    protected List<X509Certificate>           trustedCerts_         = new LinkedList<>();
    protected List<String>                    trustedCertResources_ = new LinkedList<>();
    private TrustStrategy                     sslTrustStrategy_     = null;
    protected ITraceContextTransactionFactory traceFactory_         = new NoOpContextFactory();
    protected int                             maxHttpConnections_   = 200;

    
    public AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the maximum number of concurrent HTTP connections which the client can make.
     * 
     * The default value is 200.
     * 
     * @param maxHttpConnections The maximum number of concurrent HTTP connections which the client can make.
     * 
     * @return This (fluent method).
     */
    public T withMaxHttpConnections(int maxHttpConnections)
    {
      maxHttpConnections_ = maxHttpConnections;
      
      return self();
    }
    
    public T withTraceFactory(ITraceContextTransactionFactory traceFactory)
    {
      traceFactory_ = traceFactory;
      
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
      if(rsaPemCredentialFile == null)
        return self();
      
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
    
    public T withCipherSuite(String cipherSuiteId)
    {
      CipherSuiteId id = CipherSuiteId.valueOf(cipherSuiteId);
      
      if(id == null)
        throw new IllegalArgumentException("Invalid cipher suite ID \"" + cipherSuiteId + "\"");
      
      cipherSuiteId_ = id;
      
      return self();
    }

    public T withObjectStoreUrl(URL objectStoreUrl)
    {
      objectStoreUrl_ = objectStoreUrl.toString();
      
      return self();
    }

    public T withObjectStoreUrl(String objectStoreUrl)
    {
      switch(objectStoreUrl)
      {
        case "local":
          log_.info("Using local service URLS");
          break;
        
        default:
          try
          {
            new URL(objectStoreUrl);
          }
          catch (MalformedURLException e)
          {
            throw new IllegalArgumentException("Invalid objectStoreUrl", e);
          }
      }
      objectStoreUrl_ = objectStoreUrl;
      
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

      cipherSuite_ = cipherSuiteId_ == null ? CipherSuite.getDefault() : CipherSuite.get(cipherSuiteId_);
      
      for(String resourceName : trustedCertResources_)
      {
        trustedCerts_.add(cipherSuite_.certificateFromPemResource(resourceName));
      }
      
      cookieStore_ = new BasicCookieStore();
      
      httpclient_ = HttpClients.custom()
          .setDefaultCookieStore(cookieStore_)
          .setConnectionManager(createConnectionManager())
          .build();
    }
    
    private PoolingHttpClientConnectionManager createConnectionManager()
    {
      
      PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
      
      
      if(!trustedCerts_.isEmpty() || sslTrustStrategy_ != null)
      {
        SSLConnectionSocketFactory sslsf = configureTrust();
        
        //httpBuilder.setSSLSocketFactory(sslsf);
        
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
            .register("http", new PlainConnectionSocketFactory())
            .register("https", sslsf)
            .build();
        
        connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        
      }
      else
      {
        connectionManager = new PoolingHttpClientConnectionManager();
      }
      
      connectionManager.setDefaultMaxPerRoute(maxHttpConnections_);
      connectionManager.setMaxTotal(maxHttpConnections_);
      
      return connectionManager;
    }

    private SSLConnectionSocketFactory configureTrust()
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
        
        return sslsf;
        
        
      }
      catch(GeneralSecurityException | IOException e)
      {
        throw new IllegalStateException("Failed to configure SSL trust", e);
      }
    }
  }
  

  @Override
  public void close()
  {
    try
    {
      httpClient_.close();
    }
    catch (IOException e)
    {
      log_.error("Unable to close HttpClient", e);
    }
  }

  @Override
  public CloseableHttpClient getHttpClient()
  {
    return httpClient_;
  }
  
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
  public IAllegroQueryManager fetchFeedObjects(FetchFeedObjectsRequest request)
  {
    if(request.getConsumerManager() instanceof ConsumerManager)
    {
      fetchFeedObjects(request, (ConsumerManager)request.getConsumerManager());
      
      return null;
    }
    else if(request.getConsumerManager() instanceof AsyncConsumerManager)
    {
      return fetchFeedObjects(request, (AsyncConsumerManager)request.getConsumerManager());
    }
    else
    {
      throw new BadRequestException("Unrecognised consumer manager type " + request.getConsumerManager().getClass());
    }
  }
  
  private IAllegroQueryManager fetchFeedObjects(FetchFeedObjectsRequest request, AsyncConsumerManager consumerManager)
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
        .withTraceContextTransactionFactory(traceFactory_)
        .withUnprocessableMessageConsumer(unprocessableConsumer)
        .withSubscription(new AllegroSubscription(request, this))
        .withSubscriberThreadPoolSize(consumerManager.getSubscriberThreadPoolSize())
        .withHandlerThreadPoolSize(consumerManager.getHandlerThreadPoolSize())
      .build();
    
    return subscriberManager;
  }

  private void fetchFeedObjects(FetchFeedObjectsRequest request, ConsumerManager consumerManager)
  {
    try (ITraceContextTransaction parentTraceTransaction = traceFactory_
        .createTransaction("fetchObjectVersionsSet", String.valueOf(request.hashCode())))
    {
      ITraceContext parentTrace = parentTraceTransaction.open();

      for (FeedQuery query : request.getQueryList())
      {
        Hash feedHash = query.getHash(getUserId());
        
        try(ITraceContextTransaction traceTransaction = parentTrace.createSubContext("FetchFeed", feedHash.toString()))
        {
          ITraceContext trace = traceTransaction.open();
          
          List<IFeedObject> messages  = objectApiClient_.newFeedsFeedHashObjectsPostHttpRequestBuilder()
              .withFeedHash(feedHash)
              .withCanonPayload(new FeedRequest.Builder()
                  .withMaxItems(query.getMaxItems() != null ? query.getMaxItems() : 1)
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
              consume(consumerManager, message.getPayload(), trace);
                
              builder.withDelete(new FeedObjectDelete.Builder()
                  .withReceiptHandle(message.getReceiptHandle())
                  .build()
                  );
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
              
              consumerManager.getUnprocessableMessageConsumer().consume(message.getPayload(), trace, "Unprocessable message, aborted", e);
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
      }
    }
    
    consumerManager.closeConsumers();
  }

  void consume(AbstractConsumerManager consumerManager, Object payload, ITraceContext trace) throws RetryableConsumerException, FatalConsumerException
  {
    consumerManager.consume(payload, trace, null);
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
        .withPartitionSelections(request.getPartitionSelections(getUserId()))
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

  @Override
  public IAllegroQueryManager fetchPartitionObjects(FetchPartitionObjectsRequest request)
  {
    if(request.getConsumerManager() instanceof ConsumerManager)
    {
      fetchPartitionObjects(request, (ConsumerManager)request.getConsumerManager());
      
      return null;
    }
    else if(request.getConsumerManager() instanceof AsyncConsumerManager)
    {
      return fetchPartitionObjects(request, (AsyncConsumerManager)request.getConsumerManager());
    }
    else
    {
      throw new BadRequestException("Unrecognised consumer manager type " + request.getConsumerManager().getClass());
    }
  }
  
  private IAllegroQueryManager fetchPartitionObjects(FetchPartitionObjectsRequest request, AsyncConsumerManager consumerManager)
  {

    AsyncPartitionQueryListManager subscriberManager = new AsyncPartitionQueryListManager.Builder()
        .withAllegroApi(this)
        .withHttpClient(httpClient_)
        .withObjectApiClient(objectApiClient_)
        .withTraceContextTransactionFactory(traceFactory_)
        .withRequest(request)
        .withConsumerManager(consumerManager)
      .build();
    
    return subscriberManager;
  }

  private void fetchPartitionObjects(FetchPartitionObjectsRequest request, ConsumerManager consumerManager)
  {
    try (ITraceContextTransaction parentTraceTransaction = traceFactory_
        .createTransaction("fetchPartitionSetObjects", String.valueOf(request.hashCode())))
    {
      ITraceContext parentTrace = parentTraceTransaction.open();

      for (PartitionQuery query : request.getQueryList())
      {
        Integer limit           = query.getMaxItems();
        int     remainingItems  = limit == null ? 0 : limit;
        
        if (limit != null && remainingItems <= 0)
          break;

        Hash    partitionHash = query.getHash(getUserId());
        String  after         = query.getAfter();

        try (ITraceContextTransaction traceTransaction = parentTrace.createSubContext("fetchPartitionObjects",
            partitionHash.toString()))
        {
          ITraceContext trace = traceTransaction.open();

          do
          {
            PartitionsPartitionHashPageGetHttpRequestBuilder pageRequest = objectApiClient_
                .newPartitionsPartitionHashPageGetHttpRequestBuilder()
                  .withPartitionHash(partitionHash)
                  .withAfter(after)
                  .withSortKeyPrefix(query.getSortKeyPrefix())
                  .withScanForwards(query.getScanForwards());

            if (limit != null)
              pageRequest.withLimit(remainingItems);

            IPageOfStoredApplicationObject page = pageRequest
                .build()
                .execute(httpClient_);

            for (IAbstractStoredApplicationObject item : page.getData())
            {
              try
              {
                consumerManager.consume(item, trace, this);
              }
              catch (RetryableConsumerException | FatalConsumerException e)
              {
                consumerManager.getUnprocessableMessageConsumer().consume(item, trace,
                    "Failed to process message", e);
              }
              remainingItems--;
            }

            after = null;
            IPagination pagination = page.getPagination();

            if (pagination != null)
            {
              ICursors cursors = pagination.getCursors();

              if (cursors != null)
                after = cursors.getAfter();
            }
          } while (after != null && (limit == null || remainingItems > 0));
        }
      }
    }
  }
  

  @Override
  public PartitionObjectPage fetchPartitionObjectPage(PartitionQuery query)
  {
    Hash          partitionHash   = query.getHash(getUserId());
    Integer       limit           = query.getMaxItems();
    int           remainingItems  = limit == null ? 0 : limit;
    String        after           = query.getAfter();

    PartitionsPartitionHashPageGetHttpRequestBuilder pageRequest = objectApiClient_
        .newPartitionsPartitionHashPageGetHttpRequestBuilder()
          .withPartitionHash(partitionHash)
          .withAfter(after)
          .withSortKeyPrefix(query.getSortKeyPrefix())
          .withScanForwards(query.getScanForwards());

    if (limit != null)
      pageRequest.withLimit(remainingItems);

    IPageOfStoredApplicationObject page = pageRequest
        .build()
        .execute(httpClient_);
    
    return new PartitionObjectPage(this, partitionHash, query, page);
  }

  @Override
  public EncryptedApplicationObjectBuilder newEncryptedApplicationObjectBuilder()
  {
    return new EncryptedApplicationObjectBuilder();
  }
  
  @Override
  public EncryptedApplicationObjectUpdater newEncryptedApplicationObjectUpdater(IStoredApplicationObject existingObject)
  {
    return new EncryptedApplicationObjectUpdater(existingObject);
  }

  /**
   * Builder for application type FundamentalObjects which takes an existing ApplicationObject for which a new
   * version is to be created.
   * 
   * @author Bruce Skingle
   *
   */
  public class ApplicationObjectDeleter extends EntityBuilder<ApplicationObjectDeleter, IDeletedApplicationObject>
  {
    private DeletedApplicationObject.Builder builder_;
    
    /**
     * Constructor.
     * 
     * @param existingObject An existing Application Object for which is to be deleted. 
     */
    public ApplicationObjectDeleter(IApplicationObjectPayload existingObject)
    {
      this(existingObject.getStoredApplicationObject());
    }
    
    /**
     * Constructor.
     * 
     * @param existingObject An existing Application Object for which is to be deleted. 
     */
    public ApplicationObjectDeleter(IStoredApplicationObject existingObject)
    {
      super(ApplicationObjectDeleter.class, existingObject);
      
      IStoredApplicationObject existing = existingObject;
      
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
    protected IDeletedApplicationObject construct()
    {
      return builder_.build();
    }
  }
  
  @Override
  public ApplicationObjectDeleter newApplicationObjectDeleter(IStoredApplicationObject existingObject)
  {
    return new ApplicationObjectDeleter(existingObject);
  }

  /**
   * Super class for ApplicationObject builders which take an already encrypted payload.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The concrete type for fluent methods.
   */
  abstract class BaseEncryptedApplicationObjectBuilder<T extends BaseEncryptedApplicationObjectBuilder<T>> extends EntityBuilder<T, IStoredApplicationObject>
  {
    protected final StoredApplicationObject.Builder  builder_ = new StoredApplicationObject.Builder();
    
    BaseEncryptedApplicationObjectBuilder(Class<T> type)
    {
      super(type);
    }
    
    BaseEncryptedApplicationObjectBuilder(Class<T> type,
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
      
      super.validate();
    }
  }
  
  /**
   * Builder for Application Objects.
   * 
   * @author Bruce Skingle
   *
   */
  public class EncryptedApplicationObjectBuilder extends BaseEncryptedApplicationObjectBuilder<EncryptedApplicationObjectBuilder>
  {
    EncryptedApplicationObjectBuilder()
    {
      super(EncryptedApplicationObjectBuilder.class);
    }

    /**
     * Set the id of the thread with whose content key this object will be encrypted.
     * 
     * @param threadId The id of the thread with whose content key this object will be encrypted.
     * 
     * @return This (fluent method).
     */
    public EncryptedApplicationObjectBuilder withThreadId(ThreadId threadId)
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
    public EncryptedApplicationObjectBuilder withPartition(Hash partitionHash)
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
    public EncryptedApplicationObjectBuilder withPartition(PartitionId partitionId)
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
    public EncryptedApplicationObjectBuilder withPartition(IPartition partition)
    {
      builder_.withPartitionHash(partition.getId().getHash());
      
      return self();
    }
    
    /**
     * Set the already encrypted object payload and header.
     * 
     * @param payload The encrypted object payload and header.
     * 
     * @return This (fluent method).
     */
    public EncryptedApplicationObjectBuilder withEncryptedPayloadAndHeader(IEncryptedApplicationPayloadAndHeader payload)
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
    public EncryptedApplicationObjectBuilder withEncryptedPayload(IEncryptedApplicationPayload payload)
    {
      builder_.withEncryptedPayload(payload.getEncryptedPayload());
      builder_.withRotationId(payload.getRotationId());
      builder_.withCipherSuiteId(payload.getCipherSuiteId());
      builder_.withThreadId(payload.getThreadId());
      
      return self();
    }
    
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
  public class EncryptedApplicationObjectUpdater extends BaseEncryptedApplicationObjectBuilder<EncryptedApplicationObjectUpdater>
  {
    /**
     * Constructor.
     * 
     * @param existing An existing Application Object for which a new version is to be created. 
     */
    public EncryptedApplicationObjectUpdater(IStoredApplicationObject existing)
    {
      super(EncryptedApplicationObjectUpdater.class, existing);
      
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
    }
    
    /**
     * Set the already encrypted object payload and header.
     * 
     * @param payload The encrypted object payload and header.
     * 
     * @return This (fluent method).
     */
    public EncryptedApplicationObjectUpdater withEncryptedPayloadAndHeader(IEncryptedApplicationPayloadAndHeader payload)
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
    public EncryptedApplicationObjectUpdater withEncryptedPayload(IEncryptedApplicationPayload payload)
    {
      if(!builder_.getThreadId().equals(payload.getThreadId()))
        throw new IllegalArgumentException("The threadId of an object cannot be changed. The object being updated has thread ID " + builder_.getThreadId());
      
      builder_.withEncryptedPayload(payload.getEncryptedPayload());
      builder_.withRotationId(payload.getRotationId());
      builder_.withCipherSuiteId(payload.getCipherSuiteId());
      
      return self();
    }

    @Override
    protected IStoredApplicationObject construct()
    {
      return builder_.build();
    }
  }
  
  @Override
  public IEntitlement fetchEntitlement(FetchEntitlementRequest request)
  {
    return authzApiClient_.newEntitlementsEntitlementHashGetHttpRequestBuilder()
      .withEntitlementHash(request.getHash(getUserId()))
      .build()
      .execute(httpClient_);
  }
  
  @Override
  public IEntitlement fetchEntitlement(IServiceEntitlementSpecOrIdProvider entitlementSpec)
  {
    return authzApiClient_.newEntitlementsEntitlementHashGetHttpRequestBuilder()
        .withEntitlementHash(entitlementSpecAdaptor_.getEntitlementId(entitlementSpec).getHash())
        .build()
        .execute(httpClient_);
  }
  
  @Override
  public IPodEntitlementMapping upsertPodEntitlementMapping(IGeneralEntitlementSpec entitlementSpec, PodId subjectPodId, EntitlementAction action)
  {
    IEntitlementId entitlementId = entitlementSpecAdaptor_.getEntitlementId(entitlementSpec);
    
    IPodEntitlementMapping payload = new PodEntitlementMapping.Builder()
        .withEntitlementId(entitlementId)
        .withPodId(subjectPodId)
        .withAction(action)
        .withEffectiveDate(Instant.now())
        .build();
    
    authzApiClient_.newPodsPodIdEntitlementsEntitlementHashPutHttpRequestBuilder()
        .withPodId(subjectPodId)
        .withEntitlementHash(entitlementId.getHash())
        .withCanonPayload(payload)
        .build()
        .execute(httpClient_);
    
    return payload;
  }
  
  @Override
  public IUserEntitlementMapping upsertUserEntitlementMapping(IGeneralEntitlementSpec entitlementSpec, PodAndUserId subjectUserId, EntitlementAction action)
  {
    IEntitlementId entitlementId = entitlementSpecAdaptor_.getEntitlementId(entitlementSpec);
    
    IUserEntitlementMapping payload = new UserEntitlementMapping.Builder()
        .withEntitlementId(entitlementId)
        .withUserId(subjectUserId)
        .withAction(action)
        .withEffectiveDate(Instant.now())
        .build();
    
    authzApiClient_.newUsersUserIdEntitlementsEntitlementHashPutHttpRequestBuilder()
        .withUserId(subjectUserId)
        .withEntitlementHash(entitlementId.getHash())
        .withCanonPayload(payload)
        .build()
        .execute(httpClient_);
    
    return payload;
  }
  
  @Override
  public IAuthenticationProvider getJwtGenerator()
  {
    return jwtGenerator_;
  }

  @Override
  public IRequestAuthenticator<IAuthcContext> getAuthenticator()
  {
    if(authenticator_ == null)
    {
      authenticator_ = new RemoteJwtAuthenticator(authcApiClient_, httpClient_);
    }
    
    return authenticator_;
  }

  @Override
  public IEntitlementValidator getEntitlementValidator()
  {
    return entitlementValidator_;
  }

  @Override
  public IServiceInfo fetchServiceInfo(IMultiTenantService service)
  {
    ServiceId serviceId = ServiceId.newBuilder().build(service.getName());
  
    if(!serviceMap_.containsKey(serviceId))
    {
      try
      {
        IServiceInfo serviceInfo = authcApiClient_.newServicesServiceIdGetHttpRequestBuilder()
            .withServiceId(serviceId)
            .build()
            .execute(httpClient_);
        
        serviceMap_.put(serviceId, serviceInfo);
      }
      catch(NotFoundException e)
      {
        serviceMap_.put(serviceId, null);
        
        throw e;
      }
    }
    IServiceInfo serviceInfo = serviceMap_.get(serviceId);
    
    if(serviceInfo == null)
      throw new NotFoundException("No such service \"" + serviceId + "\"");
    
    return serviceInfo;
  }

  @Override
  public IAbstractStoredApplicationObject fetchAbsolute(Hash absoluteHash)
  {
    return fetch(absoluteHash, false);
  }
  
  @Override
  public IStoredApplicationObject fetchCurrent(Hash absoluteHash)
  {
    IAbstractStoredApplicationObject result = fetch(absoluteHash, true);
    
    if(result instanceof IStoredApplicationObject)
      return (IStoredApplicationObject)result;
    
    throw new ServerErrorException("Unexpected result of type " + result.getCanonType());
  }
  
  private IAbstractStoredApplicationObject fetch(Hash objectHash, boolean currentVersion)
  {
    return objectApiClient_.newObjectsObjectHashGetHttpRequestBuilder()
        .withObjectHash(objectHash)
        .withCurrentVersion(currentVersion)
        .build()
        .execute(httpClient_);
  }

  @Override
  public void delete(IStoredApplicationObject existingObject, DeletionType deletionType)
  {
    IDeletedApplicationObject deletedObject = newApplicationObjectDeleter(existingObject)
      .withDeletionType(deletionType)
      .build()
      ;
    
    store(deletedObject);
  }

  @Override
  public @Nullable IAllegroQueryManager fetchObjectVersions(FetchObjectVersionsRequest request)
  {
    if(request.getConsumerManager() instanceof ConsumerManager)
    {
      fetchObjectVersions(request, (ConsumerManager)request.getConsumerManager());
      
      return null;
    }
    else if(request.getConsumerManager() instanceof AsyncConsumerManager)
    {
      return fetchObjectVersions(request, (AsyncConsumerManager)request.getConsumerManager());
    }
    else
    {
      throw new BadRequestException("Unrecognised consumer manager type " + request.getConsumerManager().getClass());
    }
  }
  
  private IAllegroQueryManager fetchObjectVersions(FetchObjectVersionsRequest request, AsyncConsumerManager consumerManager)
  {
    AsyncVersionQueryListManager subscriberManager = new AsyncVersionQueryListManager.Builder()
        .withAllegroApi(this)
        .withHttpClient(httpClient_)
        .withObjectApiClient(objectApiClient_)
        .withTraceContextTransactionFactory(traceFactory_)
        .withRequest(request)
        .withConsumerManager(consumerManager)
      .build();
    
    return subscriberManager;
  }

  private void fetchObjectVersions(FetchObjectVersionsRequest request, ConsumerManager consumerManager)
  {
    try (ITraceContextTransaction parentTraceTransaction = traceFactory_
        .createTransaction("fetchObjectVersionsSet", String.valueOf(request.hashCode())))
    {
      ITraceContext parentTrace = parentTraceTransaction.open();

      for (VersionQuery query : request.getQueryList())
      {
        Integer limit           = query.getMaxItems();
        int     remainingItems  = limit == null ? 0 : limit;
        
        if (limit != null && remainingItems <= 0)
          break;

        Hash    baseHash      = query.getBaseHash();
        String  after         = query.getAfter();

        try (ITraceContextTransaction traceTransaction = parentTrace.createSubContext("fetchObjectVersions",
            baseHash.toString()))
        {
          ITraceContext trace = traceTransaction.open();

          do
          {
            ObjectsObjectHashVersionsGetHttpRequestBuilder pageRequest = objectApiClient_.newObjectsObjectHashVersionsGetHttpRequestBuilder()
                .withObjectHash(baseHash)
                .withAfter(after)
                .withScanForwards(query.getScanForwards())
                ;

            if (limit != null)
              pageRequest.withLimit(remainingItems);

            IPageOfAbstractStoredApplicationObject page = pageRequest
                .build()
                .execute(httpClient_);

            for (IAbstractStoredApplicationObject item : page.getData())
            {
              try
              {
                consumerManager.consume(item, trace, this);
              }
              catch (RetryableConsumerException | FatalConsumerException e)
              {
                consumerManager.getUnprocessableMessageConsumer().consume(item, trace,
                    "Failed to process message", e);
              }
              remainingItems--;
            }

            after = null;
            IPagination pagination = page.getPagination();

            if (pagination != null)
            {
              ICursors cursors = pagination.getCursors();

              if (cursors != null)
                after = cursors.getAfter();
            }
          } while (after != null && (limit == null || remainingItems > 0));
        }
      }
    }
  }
}
