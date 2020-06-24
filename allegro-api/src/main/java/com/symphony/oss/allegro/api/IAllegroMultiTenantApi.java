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

import java.io.Closeable;
import java.util.Collection;

import javax.annotation.Nullable;

import org.apache.http.impl.client.CloseableHttpClient;

import com.symphony.oss.allegro.api.AllegroBaseApi.ApplicationObjectDeleter;
import com.symphony.oss.allegro.api.AllegroBaseApi.EncryptedApplicationObjectBuilder;
import com.symphony.oss.allegro.api.AllegroBaseApi.EncryptedApplicationObjectUpdater;
import com.symphony.oss.allegro.api.request.FetchEntitlementRequest;
import com.symphony.oss.allegro.api.request.FetchFeedObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchObjectVersionsRequest;
import com.symphony.oss.allegro.api.request.FetchPartitionObjectsRequest;
import com.symphony.oss.allegro.api.request.PartitionQuery;
import com.symphony.oss.allegro.api.request.UpsertFeedRequest;
import com.symphony.oss.allegro.api.request.UpsertPartitionRequest;
import com.symphony.oss.allegro.api.request.VersionQuery;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.exception.BadRequestException;
import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.canon.runtime.exception.PermissionDeniedException;
import com.symphony.oss.canon.runtime.http.IRequestAuthenticator;
import com.symphony.oss.canon.runtime.http.client.IAuthenticationProvider;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.allegro.canon.facade.IAllegroBaseConfiguration;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.object.canon.DeletionType;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IFeed;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.SortKey;
import com.symphony.s2.authc.model.IAuthcContext;
import com.symphony.s2.authc.model.IMultiTenantServiceRegistry;
import com.symphony.s2.authz.canon.EntitlementAction;
import com.symphony.s2.authz.canon.facade.IEntitlement;
import com.symphony.s2.authz.canon.facade.IPodEntitlementMapping;
import com.symphony.s2.authz.canon.facade.IUserEntitlementMapping;
import com.symphony.s2.authz.model.IEntitlementValidator;
import com.symphony.s2.authz.model.IGeneralEntitlementSpec;
import com.symphony.s2.authz.model.IServiceEntitlementSpecOrIdProvider;

/**
 * The public interface of the Multi-Tenant Allegro API.
 * <p>
 * The multi-tenant version of AllegroApi does not support any encryption or decryption capabilities and
 * does not require authentication to a pod. This is intended for use by multi-tenant services. For the
 * full API see {@link IAllegroApi}.
 * <p>
 * Generally methods called {@code getXXX()} return something from the local client whereas methods
 * called {@code fetchXXX()} involve a network request to some server.
 * <p>
 * @author Bruce Skingle
 */
public interface IAllegroMultiTenantApi extends IMultiTenantServiceRegistry, Closeable
{
  @Override
  void close();
  
  /**
   * @return The user ID of the user we have authenticated as.
   */
  PodAndUserId getUserId();
  
  /**
   * The session token is required in a header called sessionToken for calls to public API methods and as a cookie called
   * skey in private methods intended for use by Symphony clients.
   * <p>
   * @return The session token.
   */
  String getSessionToken();
  
  /**
   * Store the given object.
   * <p>
   * @param object An Object to be stored.
   */
  void store(IAbstractStoredApplicationObject object);
  
  /**
   * Store the given collection of objects in a single atomic transaction.
   * 
   * The underlying storage infrastructure has a limit on the number of items which can be stored in a single
   * transaction (currently 25). Each object store object requires 4 rows in the underlying database so the
   * current limit on the number of objects which can be successfully written in a single transaction is 
   * currently 25 / 4 = 6. If too many objects are passed the request will fail throwing a BadRequestException
   * and no change will have been made to the object store.
   * 
   * @param objects Objects to be stored.
   * 
   * @throws BadRequestException If the transaction is too large.
   */
  void storeTransaction(Collection<IAbstractStoredApplicationObject> objects);

  /**
   * Fetch objects from a partition.
   * <p>
   * If this method is called with a query which was created with .withScanForwards(false), then the rows will be returned
   * in descending order of sort key (i.e. reverse sequence). If the call includes an AsyncConsumerManager then the order
   * of processing is indeterminate, but if a (synchronous) ConsumerManager is passed then the objects will be observed
   * in the forwards or reverse sequence depending on whether .withScanForwards(true) or .withScanForwards(false)
   * was used respectively.
   * <p>
   * @param request   The request parameters.
   * <p>
   * @return If the invocation is asynchronous then a subscriber controller, you must call the start() method on this object and the stop()
   * method may be called for a graceful shutdown. If the invocation is synchronous then the return value is <code>null</code>.
   */
  @Nullable IAllegroQueryManager fetchPartitionObjects(FetchPartitionObjectsRequest request);

  /**
   * Fetch a page of objects from the given partition.
   * <p>
   * The returned IObjectPage allows the next and previous pages to be fetched.
   * <p>
   * Because this method is intended to be called in a paged context, the rows provided by the 
   * getData() method on the returned IObjectPage will always be in the forwards order (ascending order of sort key)
   * regardless of whether the query was specified with .withScanForwards(false) or .withBefore(String).
   * <p>
   * If such a query were made via fetchPartitionObjects(FetchPartitionObjectsRequest request) then the rows would
   * be returned in the reverse sequence.
   * <p>
   * This method makes exactly one call to the server to retrieve a page of objects.
   * <p>
   * @param query The query parameters for the objects required.
   * <p>
   * @return  A page of objects.
   */
  IObjectPage fetchPartitionObjectPage(PartitionQuery query);
  
  /**
   * Create a new EncryptedApplicationObjectBuilder.
   * <p>
   * @return A new EncryptedApplicationObjectBuilder.
   */
  EncryptedApplicationObjectBuilder newEncryptedApplicationObjectBuilder();
  
  /**
   * Create a new EncryptedApplicationObjectBuilder to create a new version of the given object.
   * <p>
   * @param existingObject An existing object for which a new version is to be created.
   * <p>
   * @return A new ApplicationObjectBuilder to create a new version of the given object.
   */
  EncryptedApplicationObjectUpdater newEncryptedApplicationObjectUpdater(IStoredApplicationObject existingObject);
  
  /**
   * Create a new ApplicationObjectDeleter to delete the given object.
   * <p>
   * @param existingObject An existing application object which is to be deleted.
   * <p>
   * @return A new ApplicationObjectDeleter to delete the given object.
   */
  ApplicationObjectDeleter newApplicationObjectDeleter(IStoredApplicationObject existingObject);
  
  /**
   * Fetch the meta-data for a sequence, or create it if it does not exist.
   * <p>
   * @param request The request parameters.
   * <p>
   * @return The sequence meta-data.
   */
  IPartition upsertPartition(UpsertPartitionRequest request);

  /**
   * Fetch an object by its absolute hash.
   * <p>
   * @param absoluteHash The hash of the required object.
   * <p>
   * @return The raw object.
   * <p>
   * @throws NotFoundException If the object does not exist. 
   */
  IAbstractStoredApplicationObject fetchAbsolute(Hash absoluteHash);

  /**
   * Fetch the current version of an object by its base hash.
   * <p>
   * @param baseHash     The base hash of the required object.
   * <p>
   * @return The raw object.
   * <p>
   * @throws NotFoundException      If the object does not exist.
   */
  IStoredApplicationObject fetchCurrent(Hash baseHash);

  /**
   * Delete the given object.
   * <p>
   * The deletion may be logical or physical.
   * <p>
   * In the case of a logical delete, the object will be deleted from any current sequences of which it
   * is a member, and a delete record will be added to any absolute sequences of which it is a member
   * and as the latest version of the object.
   * <p>
   * When performing a fetch of a logically deleted object a DeletedException (HTTP status 410) will be
   * thrown, which is a sub-class of NotFoundException (HTTP status 404) so code which does not catch
   * DeletedException specifically will see a NotFoundException which is the result which would have 
   * occurred if the object had never existed.
   * <p>
   * In the case of a physical delete, all versions of the object are physically removed as are all
   * copies of the object on all sequences.
   * <p>
   * @param item          An existing object which is to be deleted.
   * @param deletionType  The type of deletion to be performed.
   */
  void delete(IStoredApplicationObject item, DeletionType deletionType);

  /**
   * Upsert (insert or update as necessary) a feed with the given details. A feed is identified by a user ID and name tuple,
   * feeds can only be created with the userId of the creator.
   * <p>
   * This operation creates the feed if necessary and can also subscribe the feed to one or more partitions if it is not
   * already subscribed. This method is idempotent.
   * <p>
   * e.g.
   * <p>
   * <pre>{@code
      IFeed feed = allegroApi_.upsertFeed(
        new UpsertFeedRequest.Builder()
          .withName("MyFeedName")
          .withPartitionHashes(allegroApi_.getPartitionHash(
              new FetchPartitionRequest.Builder()
                .withName("MyPartitionName")
                .withOwner(ownerUserId)
                .build()
              ))
          .build()
          );
   * }</pre>
   * <p>
   * @param request The details of the feed to be created or returned.
   * <p>
   * @return The feed object.
   */
  IFeed upsertFeed(UpsertFeedRequest request);
  
  /**
   * Fetch objects from the given feed.
   * <p>
   * This method makes a synchronous or asynchronous request depending on whether the consumer provided by the request object
   * is a <code>ConsumerManager</code> or an <code>AsyncConsumerManager</code>
   * <p>
   * <h4>Synchronous Invocation</h4>
   * It is up to the server to decide how many objects to return, if there are more objects available than requested this does
   * NOT guarantee that the full number of objects requested will be returned.
   * <p>
   * e.g.
   * <pre>{@code
    allegroApi_.fetchFeedObjects(new FetchFeedObjectsRequest.Builder()
        .withQuery(new FeedQuery.Builder()
            .withName(feedName)
            .build()
            )
        .withConsumerManager(new ConsumerManager.Builder()
            .withMaxItems(10)
            .withConsumer(Object.class, (object, trace) -&gt;
            {
              System.out.println(object);
            })
            .build())
        .build()
        );
   * }</pre>
   * <p>
   * In the case of a synchronous invocation this method returns <code>null</code>
   * <p>
   * <h4>Asynchronous Invocation</h4>
   * This invocation style uses two thread pools to asynchronously fetch messages.
   * <p>
   * The start() method must be called on the returned subscriber to begin processing messages.
   * <p>
   * e.g.
   * <pre>{@code
    IAllegroQueryManager subscriber = allegroApi_.fetchFeedObjects(new FetchFeedObjectsRequest.Builder()
        .withQuery(new FeedQuery.Builder()
            .withName(feedName)
            .build()
            )
        .withConsumerManager(new AsyncConsumerManager.Builder()
          .withSubscriberThreadPoolSize(10)
          .withHandlerThreadPoolSize(90)
          .withConsumer(IToDoItem.class, (message, traceContext) -&gt;
          {
            log_.info(message.toString());
          })
          .withUnprocessableMessageConsumer((item, trace, message, cause) -&gt;
          {
            log_.error("Failed to consume message: " + message + "\nPayload:" + item, cause);
          })
          .build()
        )
      .build()
    );

    log_.info("Subscriber state: " + subscriber.getLifecycleState());
    subscriber.start();
    
    // some activity or a wait loop....
     
    
    log_.info("Stopping...");
    subscriber.stop();
    log_.info("Subscriber state: " + subscriber.getLifecycleState());
   * }</pre>
   * <p>
   * @param request The details of the request
   * <p>
   * @return If the invocation is asynchronous then a subscriber controller, you must call the start() method on this object and the stop()
   * method may be called for a graceful shutdown. If the invocation is synchronous then the return value is <code>null</code>. 
   * <p>
   */
  @Nullable IAllegroQueryManager fetchFeedObjects(FetchFeedObjectsRequest request);

  /**
   * Fetch versions of the given logical object, by its baseHash.
   * <p>
   * @param request Request parameters.
   * <p>
   * @return an IAllegroQueryManager if this is an asynchronous request otherwise null.
   */
  @Nullable IAllegroQueryManager fetchObjectVersions(FetchObjectVersionsRequest request);
  
  /**
   * Fetch a page of object versions.
   * <p>
   * The returned IObjectPage allows the next and previous pages to be fetched.
   * <p>
   * Because this method is intended to be called in a paged context, the rows provided by the 
   * getData() method on the returned IObjectPage will always be in the forwards order (ascending order of sort key)
   * regardless of whether the query was specified with .withScanForwards(false) or .withBefore(String).
   * <p>
   * If such a query were made via fetchObjectVersions(FetchObjectVersionssRequest request) then the rows would
   * be returned in the reverse sequence.
   * <p>
   * This method makes exactly one call to the server to retrieve a page of objects.
   * <p>
   * @param query The query parameters for the objects required.
   * <p>
   * @return  A page of objects.
   */
  IObjectVersionPage fetchObjectVersionsPage(VersionQuery query);

  /**
   * Fetch an entitlement.
   * <p>
   * @param request Details of the required entitlement.
   * <p>
   * @return The required entitlement.
   */
  IEntitlement fetchEntitlement(FetchEntitlementRequest request);

  /**
   * Fetch an entitlement.
   * <p>
   * @param entitlementSpec Details of the required entitlement.
   * <p>
   * @return The required entitlement.
   */
  IEntitlement fetchEntitlement(IServiceEntitlementSpecOrIdProvider entitlementSpec);

  /**
   * Upsert the mapping of an entitlement to a user (the subject).
   * <p>
   * This method can be used to grant or revoke an entitlement to or from the subject.
   * <p>
   * To remove and entitlement upsert a mapping with the action DENY.
   * <p>
   * @param entitlementSpec The entitlement.
   * @param subjectUserId   The subject.
   * @param action          One of EntitlementAction.ALLOW or EntitlementAction.DENY.
   * <p>
   * @return The upserted mapping.
   * <p>
   * @throws PermissionDeniedException If the caller is not the owner of the entitlement.
   */
  IUserEntitlementMapping upsertUserEntitlementMapping(IGeneralEntitlementSpec entitlementSpec,
      PodAndUserId subjectUserId, EntitlementAction action);

  /**
   * Upsert the mapping of an entitlement to a pod (the subject).
   * <p>
   * This method can be used to grant or revoke an entitlement to or from the subject.
   * <p>
   * To remove and entitlement upsert a mapping with the action DENY.
   * <p>
   * @param entitlementSpec The entitlement.
   * @param subjectPodId    The subject.
   * @param action          One of EntitlementAction.ALLOW or EntitlementAction.DENY.
   * <p>
   * @return The upserted mapping.
   * <p>
   * @throws PermissionDeniedException If the caller is not the owner of the entitlement.
   */
  IPodEntitlementMapping upsertPodEntitlementMapping(IGeneralEntitlementSpec entitlementSpec,
      PodId subjectPodId, EntitlementAction action);
  
  /**
   * Return An implementation of IEntitlementValidator which can be used to validate entitlements for a user.
   * <p>
   * @return An IEntitlementValidator.
   */
  IEntitlementValidator getEntitlementValidator();

  /**
   * Return an HTTP client which can be used to make requests to the object store and other multi-tenant services.
   * <p>
   * @return A CloseableHttpClient.
   */
  CloseableHttpClient getApiHttpClient();

  /**
   * Return an authenticator which can be used to authenticate requests from regular or multi-tenant service accounts.
   * <p>
   * This is the server side equivalent of the generator returned by the {@code getJwtGenerator()} method.
   * <p>
   * @return an IRequestAuthenticator.
   */
  IRequestAuthenticator<IAuthcContext> getAuthenticator();

  /**
   * Return a JWT generator which can authenticate a request made to the object store or some other multi-tenant service.
   * <p>
   * This is the client side equivalent of the authenticator returned by the {@code getAuthenticator()} method.
   * <p>
   * @return an IAuthenticationProvider.
   */
  IAuthenticationProvider getJwtGenerator();

  /**
   * Fetch a Partition object.
   * 
   * @param query The query parameters for the Partition required.
   * 
   * @return The Partition object which describes the partition.
   */
  IPartition fetchPartition(PartitionQuery query);

  /**
   * Return the ModelRegistry used by Allegro.
   * 
   * @return the ModelRegistry used by Allegro.
   */
  ModelRegistry getModelRegistry();

  /**
   * Fetch an object by its Partition and Sort key.
   * 
   * This is a convenience method which accepts a String value for the sortKey parameter.
   * 
   * @param partitionHash The Partition of which the object is a member.
   * @param sortKey       The object's sort key.
   * 
   * @return The required object.
   * 
   * @throws PermissionDeniedException If the caller does not have access to the required object.
   * @throws NotFoundException         If the requested object does not exist.
   */
  IStoredApplicationObject fetchObject(Hash partitionHash, String sortKey);

  /**
   * Fetch an object by its Partition and Sort key.
   * 
   * @param partitionHash The Partition of which the object is a member.
   * @param sortKey       The object's sort key.
   * 
   * @return The required object.
   * 
   * @throws PermissionDeniedException If the caller does not have access to the required object.
   * @throws NotFoundException         If the requested object does not exist.
   */
  IStoredApplicationObject fetchObject(Hash partitionHash, SortKey sortKey);

  /**
   * Return the current configuration.
   * 
   * @return The current configuration.
   */
  IAllegroBaseConfiguration getConfiguration();
}
