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

package com.symphony.oss.allegro.objectstore;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.allegro.api.AbstractConsumerManager;
import com.symphony.oss.allegro.api.AsyncConsumerManager;
import com.symphony.oss.allegro.api.request.FeedId;
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
import com.symphony.oss.allegro.objectstore.AllegroSqsSubscriberManager.Builder;
import com.symphony.oss.canon.runtime.EntityBuilder;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.canon.runtime.IEntityFactory;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.exception.BadRequestException;
import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.canon.runtime.exception.ServerErrorException;
import com.symphony.oss.canon.runtime.http.client.IAuthenticationProvider;
import com.symphony.oss.canon.runtime.jjwt.JwtBase;
import com.symphony.oss.commons.dom.json.ImmutableJsonObject;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.aws.sqs.SqsAction;
import com.symphony.oss.fugue.aws.sqs.SqsResponseMessage;
import com.symphony.oss.fugue.pipeline.FatalConsumerException;
import com.symphony.oss.fugue.pipeline.IThreadSafeErrorConsumer;
import com.symphony.oss.fugue.pipeline.RetryableConsumerException;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.fugue.trace.ITraceContextTransaction;
import com.symphony.oss.fugue.trace.ITraceContextTransactionFactory;
import com.symphony.oss.fugue.trace.NoOpContextFactory;
import com.symphony.oss.models.allegro.canon.AllegroModel;
import com.symphony.oss.models.allegro.canon.facade.BaseObjectStoreConfiguration;
import com.symphony.oss.models.allegro.canon.facade.ConnectionSettings;
import com.symphony.oss.models.allegro.canon.facade.IBaseObjectStoreConfiguration;
import com.symphony.oss.models.allegro.canon.facade.IConnectionSettings;
import com.symphony.oss.models.core.canon.CoreHttpModelClient;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.core.canon.HashType;
import com.symphony.oss.models.core.canon.ICursors;
import com.symphony.oss.models.core.canon.IPagination;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.object.canon.DeletionType;
import com.symphony.oss.models.object.canon.FeedRequest;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayloadAndHeader;
import com.symphony.oss.models.object.canon.IFeed;
import com.symphony.oss.models.object.canon.IFeedsEndpoint;
import com.symphony.oss.models.object.canon.IPageOfAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IPageOfStoredApplicationObject;
import com.symphony.oss.models.object.canon.IPageOfUserPermissions;
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
import com.symphony.s2.authc.model.IMultiTenantService;
import com.symphony.s2.authc.model.MultiTenantService;
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
public abstract class AllegroBaseApi implements IBaseObjectStoreApi
{
  
  private static final Logger                   log_                       = LoggerFactory.getLogger(AllegroBaseApi.class);
  private static final long                     FAILED_CONSUMER_RETRY_TIME = TimeUnit.SECONDS.toSeconds(30);
  
  private static Map<String, AllegroSqsFeedsContainer> feedsMap_  = new ConcurrentHashMap<>();
  
  final CoreHttpModelClient                  coreApiClient_;
  final ObjectHttpModelClient                objectApiClient_;
  final AuthcHttpModelClient                 authcApiClient_;
  final AuthzHttpModelClient                 authzApiClient_;
  final BaseEntitlementValidator             entitlementValidator_;
  final EntitlementSpecAdaptor               entitlementSpecAdaptor_;
  final ITraceContextTransactionFactory      traceFactory_;

  final CloseableHttpClient                  apiHttpClient_;

  private final Map<ServiceId, IServiceInfo> serviceMap_   = new HashMap<>();
  private final ModelRegistry                modelRegistry_ = new ModelRegistry()
      .withFactories(AllegroModel.FACTORIES)
      .withFactories(AuthcModel.FACTORIES)
      .withFactories(AuthzModel.FACTORIES)
      .withFactories(ObjectModel.FACTORIES)
      .withFactories(CoreModel.FACTORIES);


  IAuthenticationProvider jwtGenerator_  = new IAuthenticationProvider()
  {
    @Override
    public void authenticate(RequestBuilder builder)
    {
      builder.addHeader(JwtBase.AUTH_HEADER_KEY, JwtBase.AUTH_HEADER_VALUE_PREFIX + getApiAuthorizationToken());
    }
  };
  
  AllegroBaseApi(AbstractBuilder<?, ?> builder, String defaultApiUrl)
  {
    traceFactory_ = builder.traceFactory_;

    apiHttpClient_     = builder.getApiHttpClient();
    
    coreApiClient_  = new CoreHttpModelClient(
        getModelRegistry(),
        initUrl(defaultApiUrl, builder.getConfiguration().getApiUrl(), MultiTenantService.OBJECT), null, jwtGenerator_, null);
    
    objectApiClient_  = new ObjectHttpModelClient(
        getModelRegistry(),
        initUrl(defaultApiUrl, builder.getConfiguration().getApiUrl(), MultiTenantService.OBJECT), null, jwtGenerator_, null);
    
    authcApiClient_  = new AuthcHttpModelClient(
        getModelRegistry(),
        initUrl(defaultApiUrl, builder.getConfiguration().getApiUrl(), MultiTenantService.AUTHC), null, jwtGenerator_, null);
    
    authzApiClient_  = new AuthzHttpModelClient(
        getModelRegistry(),
        initUrl(defaultApiUrl, builder.getConfiguration().getApiUrl(), MultiTenantService.AUTHZ), null, jwtGenerator_, null);
    
    entitlementValidator_ = new BaseEntitlementValidator(apiHttpClient_, authzApiClient_, this);
    entitlementSpecAdaptor_ = new EntitlementSpecAdaptor(this);
  }

  private String initUrl(String defaultApiUrl, URL url, MultiTenantService service)
  {
    if(url == null)
      return defaultApiUrl;
    
    if(url.equals(BaseObjectStoreConfiguration.ALL_SERVICES_LOCAL_URL))
      return "http://127.0.0.1:" + service.getHttpPort();
    
    return url.toString();
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
  protected static abstract class AbstractBuilder<
    T extends AbstractBuilder<T,B>, B extends IBaseObjectStoreApi>
  extends BaseAbstractBuilder<T, B>
  {

    protected CookieStore                     cookieStore_               = new BasicCookieStore();
    protected List<IEntityFactory<?, ?, ?>>   factories_                 = new LinkedList<>();
    protected ITraceContextTransactionFactory traceFactory_              = new NoOpContextFactory();
    protected ModelRegistry                   configModelRegistry_       = new ModelRegistry()
        .withFactories(AllegroModel.FACTORIES)
        .withFactories(AuthcModel.FACTORIES);
    protected CloseableHttpClient               defaultHttpClient_;
    private CloseableHttpClient               apiHttpClient_;
    
    public AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    protected abstract IBaseObjectStoreConfiguration    getConfiguration();
    
    protected synchronized CloseableHttpClient getDefaultHttpClient()
    {
      if(defaultHttpClient_ == null)
      {
        defaultHttpClient_ = new ConnectionSettings.Builder().build().createHttpClient(cookieStore_);
      }
      
      return defaultHttpClient_;
    }
    
    public synchronized CloseableHttpClient getApiHttpClient()
    {
      if(apiHttpClient_ == null)
      {
        if(getConfiguration().getApiConnectionSettings() == null)
        {
          apiHttpClient_ = getDefaultHttpClient();
        }
        else
        {
          apiHttpClient_ = getConfiguration().getApiConnectionSettings().createHttpClient(cookieStore_);
        }
      }
      
      return apiHttpClient_;
    }
    
    public abstract T withConfiguration(Reader reader);

    public T withConfiguration(String configuration)
    {
      return withConfiguration(new StringReader(configuration));
    }
    
    public T withConfigurationFile(String fileName) throws FileNotFoundException, IOException
    {
      try(Reader reader = new FileReader(fileName))
      {
        return withConfiguration(reader);
      }
    }
    
    public T withTraceFactory(ITraceContextTransactionFactory traceFactory)
    {
      traceFactory_ = traceFactory;
      
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
  }
  
  @Override
  public final ModelRegistry getModelRegistry()
  {
    return modelRegistry_;
  }

  protected IAllegroDecryptor getDecryptor()
  {
    return null;
  }

  @Override
  public void close()
  {
    try
    {
      apiHttpClient_.close();
    }
    catch (IOException e)
    {
      log_.error("Unable to close HttpClient", e);
    }
  }

  @Override
  public CloseableHttpClient getApiHttpClient()
  {
    return apiHttpClient_;
  }
  
  @Override
  public void store(IAbstractStoredApplicationObject object)
  {
    objectApiClient_.newObjectsObjectHashPutHttpRequestBuilder()
      .withObjectHash(object.getAbsoluteHash())
      .withCanonPayload(object)
      .build()
      .execute(apiHttpClient_);
  }
  
  @Override
  public void storeTransaction(Collection<IAbstractStoredApplicationObject> objects)
  {
    objectApiClient_.newObjectsTransactionPostHttpRequestBuilder()
      .withCanonPayload(objects)
      .build()
      .execute(apiHttpClient_);
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
    List<FeedId> feedIds   = new ArrayList<>();
    
    for(FeedQuery q : request.getQueryList())
      feedIds.add(new FeedId.Builder()
          .withHash(q.getHash(getUserId()))
          .build());
    
    AllegroSqsFeedsContainer feeds  = refreshFeeds(feedIds);
    
    if(feeds.isDirect()) {
      
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

    Builder builder = new AllegroSqsSubscriberManager.Builder()
        .withFeedsContainer(feeds)
        .withTraceContextTransactionFactory(traceFactory_)
        .withHandlerThreadPoolSize(consumerManager.getHandlerThreadPoolSize())
        .withSubscriberThreadPoolSize(consumerManager.getSubscriberThreadPoolSize())
        .withUnprocessableMessageConsumer(unprocessableConsumer)
        .withSubscription(new AllegroSqsSubscription(request, feeds.getFeedIds(), getDecryptor()))
        .withModelRegistry(getModelRegistry())
        .withHttpClient(apiHttpClient_);
    
    
    IConnectionSettings connSettings = getConfiguration().getApiConnectionSettings();

    if (connSettings != null)
    {
      builder.withProxyUrl     (connSettings.getProxyUrl());
      builder.withProxyUsername(connSettings.getProxyUsername()); 
      builder.withProxyPassword(connSettings.getProxyPassword());
    }
        
    return builder.build();  
    }
    else  
      return fetchFeedObjectsFromServerAsync(request, consumerManager);

  }
  
  private void fetchFeedObjects(FetchFeedObjectsRequest request, ConsumerManager consumerManager)
  {

    try (ITraceContextTransaction parentTraceTransaction = traceFactory_ .createTransaction("fetchObjectVersionsSet", String.valueOf(request.hashCode())))
    {
      parentTraceTransaction.open();

      List<FeedId> feedIds        = new ArrayList<>();
      List<FeedQuery> queries = new ArrayList<>();
      
      for(FeedQuery q : request.getQueryList())
      {
        Hash hash = q.getHash(getUserId());
        
          feedIds.add(new FeedId.Builder()
              .withHash(hash)
              .build());
          queries.add(q);     
      }

      AllegroSqsFeedsContainer feeds = refreshFeeds(feedIds);
      
      if(feeds.isDirect()) 
      {
        try (ITraceContextTransaction subparentTraceTransaction = traceFactory_
            .createTransaction("fetchObjectVersionsSet", String.valueOf(request.hashCode())))
        {
          ITraceContext subParentTrace = parentTraceTransaction.open();

          for (FeedQuery query : queries)
          {
            Hash feedHash = query.getHash(getUserId());
            
            try(ITraceContextTransaction traceTransaction = subParentTrace.createSubContext("FetchFeed", feedHash.toString()))
            {
              ITraceContext trace = traceTransaction.open();

              List<SqsResponseMessage> messages = new AllegroSqsRequestBuilder(this, feeds.getEndpoint())
                  .withFeedHash(feedHash.toString())
                  .withAction(SqsAction.RECEIVE)
                  .withMaxNumberOfMessages(query.getMaxItems() != null ? query.getMaxItems() : 1)
                  .withWaitTimeSeconds(0)          
                .execute(apiHttpClient_);
               
              int ackCnt = 0;
              
              ArrayList<SqsResponseMessage> recv_messages = new ArrayList<>();
              
              for(SqsResponseMessage message : messages)
              {
                try
                {
                  IEntity entity = getModelRegistry().parseOne(new StringReader(message.getPayload()));

                  if (entity instanceof IAbstractStoredApplicationObject)
                  {
                    IAbstractStoredApplicationObject object = (IAbstractStoredApplicationObject) entity;
                    consume(consumerManager, object, trace);
                    
                    recv_messages.addAll(new AllegroSqsRequestBuilder(this, feeds.getEndpoint())
                        .withFeedHash(feedHash.toString())
                        .withAction(SqsAction.DELETE)
                        .withReceiptHandle(message.getReceiptHandle())
                      .execute(apiHttpClient_));
                    
                    ackCnt++;

                  }
                  else
                  {
                    log_.error("Retrieved unexpected feed entity of type " + entity.getCanonType());
                  }                 
                }
                catch(RetryableConsumerException e)
                {
                  long delay = e.getRetryTime() == null || e.getRetryTimeUnit() == null ? FAILED_CONSUMER_RETRY_TIME : e.getRetryTimeUnit().toSeconds(e.getRetryTime());
                  
                  log_.warn("Transient processing failure, will retry (forever)", e);

                  recv_messages.addAll(new AllegroSqsRequestBuilder(this, feeds.getEndpoint())
                      .withFeedHash(feedHash.toString())
                      .withAction(SqsAction.EXTEND)
                      .withReceiptHandle(message.getReceiptHandle())
                      .withVisibilityTimeout((int)delay)
                    .execute(apiHttpClient_));
                  
                  ackCnt++;
                }
                catch (RuntimeException  e)
                {
                  log_.warn("Unexpected processing failure, will retry (forever)", e);

                  recv_messages.addAll(new AllegroSqsRequestBuilder(this, feeds.getEndpoint())
                      .withFeedHash(feedHash.toString())
                      .withAction(SqsAction.EXTEND)
                      .withReceiptHandle(message.getReceiptHandle())
                      .withVisibilityTimeout((int)FAILED_CONSUMER_RETRY_TIME)
                    .execute(apiHttpClient_));
                  
                  ackCnt++;
                }
                catch (FatalConsumerException e)
                {
                  log_.error("Unprocessable message, aborted", e);
        
                  trace.trace("MESSAGE_IS_UNPROCESSABLE");
                  
                  recv_messages.addAll(new AllegroSqsRequestBuilder(this, feeds.getEndpoint())
                      .withFeedHash(feedHash.toString())
                      .withAction(SqsAction.DELETE)
                      .withReceiptHandle(message.getReceiptHandle())
                    .execute(apiHttpClient_));
                  
                  ackCnt++;
                  
                  consumerManager.getUnprocessableMessageConsumer().consume(message.getPayload(), trace, "Unprocessable message, aborted", e);
                }
              }
//            System.out.println("DELETING TOOK "+(System.currentTimeMillis() - start));
              if(ackCnt>0)
              {
                // Delete (ACK) the consumed messages
                messages = recv_messages;
              }
            }
          }
      }
    }
    else
      fetchFeedObjectsFromServerSync(request, consumerManager);
    }
    
    consumerManager.closeConsumers();
  }
  
  private AllegroSqsFeedsContainer refreshFeeds(List<FeedId> feedIds) 
  {     
    StringBuilder sb = new StringBuilder();
    
    for(FeedId feed : feedIds) 
      sb.append(feed.getHash(getUserId()));
    
    String key = sb.toString();

    AllegroSqsFeedsContainer provider = feedsMap_.get(key);
    if(provider == null)  
       feedsMap_.put(key, provider = new AllegroSqsFeedsContainer(feedIds, getUserId(), this));
    
    provider.refresh();
    
    return provider;
  }

 private IAllegroQueryManager fetchFeedObjectsFromServerAsync(FetchFeedObjectsRequest request, AsyncConsumerManager consumerManager)
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
      .withHttpClient(apiHttpClient_)
      .withObjectApiClient(objectApiClient_)
      .withTraceContextTransactionFactory(traceFactory_)
      .withUnprocessableMessageConsumer(unprocessableConsumer)
      .withSubscription(new AllegroSubscription(request, getUserId(), getDecryptor()))
      .withSubscriberThreadPoolSize(consumerManager.getSubscriberThreadPoolSize())
      .withHandlerThreadPoolSize(consumerManager.getHandlerThreadPoolSize())
    .build();
  
  return subscriberManager;
}

  private void fetchFeedObjectsFromServerSync(FetchFeedObjectsRequest request, ConsumerManager consumerManager)
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
              .execute(apiHttpClient_);
          
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
                .execute(apiHttpClient_);
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
        .withExpiryTime(request.getExpiryTime())
        .build())
      .build()
      .execute(apiHttpClient_)
      ;
  }

  IFeedsEndpoint refreshFeeds(Collection<FeedId> feedIds)
  { 
    ArrayList<Hash> feedHashes = new ArrayList<>();
    
    for(FeedId id : feedIds)
      feedHashes.add(id.getHash(getUserId()));
    
    return objectApiClient_.newFeedsEndpointPostHttpRequestBuilder()
        .withCanonPayload(feedHashes)
      .build()
      .execute(apiHttpClient_)
      ;
  }
  
  @Override
  public void deleteFeed(FeedId feedId)
  {    
     objectApiClient_.newFeedsFeedHashDeleteHttpRequestBuilder()
        .withFeedHash(feedId.getHash(getUserId()))
      .build()
      .execute(apiHttpClient_)
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
      .execute(apiHttpClient_)
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
        .withHttpClient(apiHttpClient_)
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
        int     pageLimit       = query.getPageLimit() == null || query.getPageLimit()<=0 ? 2000 : query.getPageLimit();
        
        if (limit != null && remainingItems <= 0)
          break;

        Hash    partitionHash = query.getHash(getUserId());
        String  after         = query.getAfter();

        try (ITraceContextTransaction traceTransaction = parentTrace.createSubContext("fetchPartitionObjects",
            partitionHash.toString()))
        {
          ITraceContext trace = traceTransaction.open();
          trace.trace("Request started");
          int itemsSize = 0;
          
          do
          {
            PartitionsPartitionHashPageGetHttpRequestBuilder pageRequest = objectApiClient_
                .newPartitionsPartitionHashPageGetHttpRequestBuilder()
                  .withPartitionHash(partitionHash)
                  .withAfter(after)
                  .withSortKeyPrefix(query.getSortKeyPrefix())
                  .withScanForwards(query.getScanForwards());

            pageRequest.withLimit(limit==null? pageLimit : Math.min(remainingItems, pageLimit));
            trace.trace("Excuting request");
            IPageOfStoredApplicationObject page = pageRequest
                .build()
                .execute(apiHttpClient_);
            
           itemsSize += page.getData() != null ? page.getData().size() : 0;
           
           trace.trace("Fetched items: "+itemsSize);

            for (IAbstractStoredApplicationObject item : page.getData())
            {
              try
              {
                consumerManager.consume(item, trace, getDecryptor());
              }
              catch (RetryableConsumerException | FatalConsumerException e)
              {
                consumerManager.getUnprocessableMessageConsumer().consume(item, trace,
                    "Failed to process message", e);
              }
              remainingItems--;
            }
            
            trace.trace("Consumed all items "+page.getData().size());

            after = null;
            IPagination pagination = page.getPagination();

            if (pagination != null)
            {
              ICursors cursors = pagination.getCursors();

              if (cursors != null)
                after = cursors.getAfter();
            }
          } while (after != null && (limit == null || remainingItems > 0));
          trace.trace("Request terminated");
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
        .execute(apiHttpClient_);
    
    return new PartitionObjectPage(this, partitionHash, query, page);
  }
  
  @Override
  public IPartition fetchPartition(PartitionQuery query)
  {
    Hash          partitionHash   = query.getHash(getUserId());

    return objectApiClient_
        .newPartitionsPartitionHashGetHttpRequestBuilder()
          .withPartitionHash(partitionHash)
          .build()
        .execute(apiHttpClient_);
  }
  
  @Override
  public IPageOfUserPermissions fetchPartitionUsers(PartitionQuery query)
  {
    Hash          partitionHash   = query.getHash(getUserId());

    return objectApiClient_
        .newPartitionsPartitionHashUsersGetHttpRequestBuilder()
         .withPartitionHash(partitionHash)
         .build()
        .execute(apiHttpClient_);

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
      .execute(apiHttpClient_);
  }
  
  @Override
  public IEntitlement fetchEntitlement(IServiceEntitlementSpecOrIdProvider entitlementSpec)
  {
    return authzApiClient_.newEntitlementsEntitlementHashGetHttpRequestBuilder()
        .withEntitlementHash(entitlementSpecAdaptor_.getEntitlementId(entitlementSpec).getHash())
        .build()
        .execute(apiHttpClient_);
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
        .execute(apiHttpClient_);
    
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
        .execute(apiHttpClient_);
    
    return payload;
  }
  
  @Override
  public IAuthenticationProvider getJwtGenerator()
  {
    return jwtGenerator_;
  }

  @Override
  public AuthcHttpModelClient getAuthcHttpModelClient()
  {
    return authcApiClient_;
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
            .execute(apiHttpClient_);
        
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
        .execute(apiHttpClient_);
  }
  
  @Override
  public IStoredApplicationObject fetchObject(Hash partitionHash, String sortKey)
  {
    return fetchObject(partitionHash, SortKey.newBuilder().build(sortKey));
  }
  
  @Override
  public IStoredApplicationObject fetchObject(Hash partitionHash, SortKey sortKey)
  {
    return objectApiClient_.newPartitionsPartitionHashSortKeyGetHttpRequestBuilder()
      .withPartitionHash(partitionHash)
      .withSortKey(sortKey)
      .build()
      .execute(apiHttpClient_);
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
  public ObjectVersionPage fetchObjectVersionsPage(VersionQuery query)
  {
    Hash    baseHash      = query.getBaseHash();
    String  after         = query.getAfter();
    
    ObjectsObjectHashVersionsGetHttpRequestBuilder pageRequest = objectApiClient_.newObjectsObjectHashVersionsGetHttpRequestBuilder()
        .withObjectHash(baseHash)
        .withAfter(after)
        .withScanForwards(query.getScanForwards())
        .withLimit(query.getMaxItems())
        ;

    IPageOfAbstractStoredApplicationObject page = pageRequest
        .build()
        .execute(apiHttpClient_);
    
    return new ObjectVersionPage(this, query, page);
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
        .withHttpClient(apiHttpClient_)
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
                .execute(apiHttpClient_);

            for (IAbstractStoredApplicationObject item : page.getData())
            {
              try
              {
                consumerManager.consume(item, trace, getDecryptor());
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
