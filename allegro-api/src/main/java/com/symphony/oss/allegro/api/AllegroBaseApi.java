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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

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
import org.symphonyoss.s2.canon.runtime.ModelRegistry;
import org.symphonyoss.s2.canon.runtime.exception.BadRequestException;
import org.symphonyoss.s2.canon.runtime.http.client.IAuthenticationProvider;
import org.symphonyoss.s2.canon.runtime.jjwt.JwtBase;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fault.TransientTransactionFault;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.core.trace.NoOpContextFactory;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

import com.symphony.oss.allegro.api.AllegroApi.AbstractBuilder;
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectBuilder;
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectDeleter;
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectUpdater;
import com.symphony.oss.allegro.api.AllegroApi.EncryptedApplicationPayloadAndHeaderBuilder;
import com.symphony.oss.allegro.api.AllegroApi.EncryptedApplicationPayloadBuilder;
import com.symphony.oss.allegro.api.query.IAllegroQueryManager;
import com.symphony.oss.allegro.api.request.AsyncConsumerManager;
import com.symphony.oss.allegro.api.request.ConsumerManager;
import com.symphony.oss.allegro.api.request.FeedQuery;
import com.symphony.oss.allegro.api.request.FetchFeedObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchObjectVersionsRequest;
import com.symphony.oss.allegro.api.request.FetchPartitionObjectsRequest;
import com.symphony.oss.allegro.api.request.PartitionQuery;
import com.symphony.oss.allegro.api.request.UpsertFeedRequest;
import com.symphony.oss.allegro.api.request.UpsertPartitionRequest;
import com.symphony.oss.models.chat.canon.ChatModel;
import com.symphony.oss.models.core.canon.CoreHttpModelClient;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.CryptoModel;
import com.symphony.oss.models.crypto.cipher.CipherSuite;
import com.symphony.oss.models.crypto.cipher.ICipherSuite;
import com.symphony.oss.models.internal.km.canon.KmInternalModel;
import com.symphony.oss.models.internal.pod.canon.PodInternalModel;
import com.symphony.oss.models.object.canon.DeletionType;
import com.symphony.oss.models.object.canon.FeedRequest;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IFeed;
import com.symphony.oss.models.object.canon.IUserPermissionsRequest;
import com.symphony.oss.models.object.canon.ObjectHttpModelClient;
import com.symphony.oss.models.object.canon.ObjectModel;
import com.symphony.oss.models.object.canon.UserPermissionsRequest;
import com.symphony.oss.models.object.canon.facade.FeedObjectDelete;
import com.symphony.oss.models.object.canon.facade.FeedObjectExtend;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IFeedObject;
import com.symphony.oss.models.object.canon.facade.IFeedObjectExtend;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.pod.canon.PodModel;

public abstract class AllegroBaseApi implements IAllegroBaseApi
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

  final ITraceContextTransactionFactory traceContextFactory_;
  
  AllegroBaseApi(AbstractBuilder<?, ?> builder)
  {
    
    traceContextFactory_ = new NoOpContextFactory();
    
    modelRegistry_ = new ModelRegistry()
        .withFactories(ObjectModel.FACTORIES)
        .withFactories(CoreModel.FACTORIES)
        ;
    
    for(IEntityFactory<?, ?, ?> factory : builder.factories_)
      modelRegistry_.withFactories(factory);


    httpClient_     = builder.httpclient_;
    
    coreApiClient_  = new CoreHttpModelClient(
        modelRegistry_,
        builder.objectStoreUrl_, null, jwtGenerator_);
    
    objectApiClient_  = new ObjectHttpModelClient(
        modelRegistry_,
        builder.objectStoreUrl_, null, jwtGenerator_);
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
    protected CipherSuiteId                 cipherSuiteId_;
    protected ICipherSuite                  cipherSuite_;
    protected CloseableHttpClient           httpclient_;
    protected URL                           objectStoreUrl_;
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
        .withTraceContextTransactionFactory(traceContextFactory_)
        .withUnprocessableMessageConsumer(unprocessableConsumer)
        .withSubscription(new AllegroSubscription(request, this))
        .withSubscriberThreadPoolSize(consumerManager.getSubscriberThreadPoolSize())
        .withHandlerThreadPoolSize(consumerManager.getHandlerThreadPoolSize())
      .build();
    
    return subscriberManager;
  }

  private void fetchFeedObjects(FetchFeedObjectsRequest request, ConsumerManager consumerManager)
  {
    try (ITraceContextTransaction parentTraceTransaction = traceContextFactory_
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
              consumerManager.consume(message.getPayload(), trace, this);
                
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

  @Override
  public IAllegroQueryManager fetchPartitionObjects(FetchPartitionObjectsRequest request)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IObjectPage fetchPartitionObjectPage(PartitionQuery query)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EncryptedApplicationPayloadBuilder newEncryptedApplicationPayloadBuilder()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public EncryptedApplicationPayloadAndHeaderBuilder newEncryptedApplicationPayloadAndHeaderBuilder()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApplicationObjectBuilder newApplicationObjectBuilder()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApplicationObjectUpdater newApplicationObjectUpdater(IApplicationObjectPayload existingObject)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ApplicationObjectDeleter newApplicationObjectDeleter(IStoredApplicationObject existingObject)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IAbstractStoredApplicationObject fetchAbsolute(Hash absoluteHash)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IStoredApplicationObject fetchCurrent(Hash absoluteHash)
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(IStoredApplicationObject item, DeletionType deletionType)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public IAllegroQueryManager fetchObjectVersions(FetchObjectVersionsRequest request)
  {
    // TODO Auto-generated method stub
    return null;
  }

}
