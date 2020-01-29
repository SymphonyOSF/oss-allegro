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

import java.security.cert.X509Certificate;
import java.util.List;

import javax.annotation.Nullable;

import org.symphonyoss.s2.canon.runtime.exception.NotFoundException;
import org.symphonyoss.s2.common.fluent.IFluent;

import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectBuilder;
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectDeleter;
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectUpdater;
import com.symphony.oss.allegro.api.query.IAllegroQueryManager;
import com.symphony.oss.allegro.api.request.FetchFeedMessagesRequest;
import com.symphony.oss.allegro.api.request.FetchFeedObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchObjectVersionsRequest;
import com.symphony.oss.allegro.api.request.FetchPartitionObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchRecentMessagesRequest;
import com.symphony.oss.allegro.api.request.PartitionQuery;
import com.symphony.oss.allegro.api.request.UpsertFeedRequest;
import com.symphony.oss.allegro.api.request.UpsertPartitionRequest;
import com.symphony.oss.models.allegro.canon.facade.ChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.internal.pod.canon.AckId;
import com.symphony.oss.models.internal.pod.canon.FeedId;
import com.symphony.oss.models.object.canon.DeletionType;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IFeed;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.pod.canon.IUserV2;

/**
 * The public interface of the Allegro API.
 * 
 * @author Bruce Skingle
 *
 */
public interface IAllegroApi extends IFluent<IAllegroApi>, IFundamentalOpener
{
  /** Permission value for no access */
  static final int   PERMISSION_NONE       = 0;
  
  /** Permission value for read access */
  static final int   PERMISSION_READ       = 1<<0;
  
  static final String SYMPHONY_DEV_QA_ROOT_CERT         = "/certs/symphony/devQaRoot.pem";
  static final String SYMPHONY_DEV_QA_INTERMEDIATE_CERT = "/certs/symphony/devQaIntermediate.pem";
  static final String SYMPHONY_DEV_CERT                 = "/certs/symphony/dev.pem";
  
  
  /**
   * Force authentication.
   */
  void authenticate();

  /**
   * 
   * @return Session info for the session with the pod.
   */
  IUserV2 getSessioninfo();

  /**
   * 
   * @return Info for the logged in user.
   */
  IUserV2 getUserInfo();
  
  /**
   * Fetch a chat message by its ID.
   * 
   * @param messageId The ID of the required message.
   * 
   * @return The required message.
   * 
   * @throws NotFoundException      If the object does not exist.
   */
  String getMessage(String messageId);

  /**
   * Fetch recent messages from a thread (conversation).
   * 
   * This implementation retrieves messages from the pod.
   * 
   * @param request   A request object containing the threadId and other parameters.
   * 
   */
  void fetchRecentMessagesFromPod(FetchRecentMessagesRequest request);

  /**
   * Send the given chat message.
   * 
   * @param chatMessage A message to be sent.
   */
  void sendMessage(IChatMessage chatMessage);

  /**
   * 
   * @return The pod certificate of the pod we are connected to.
   */
  X509Certificate getPodCert();

  /**
   * @return The user ID of the user we have authenticated as.
   */
  PodAndUserId getUserId();

  /**
   * Store the given object.
   * 
   * @param object An Object to be stored.
   */
  void store(IAbstractStoredApplicationObject object);

  /**
   * 
   * @return A new builder for a chat message.
   */
  ChatMessage.Builder newChatMessageBuilder();

  /**
   * Fetch objects from a partition.
   * 
   * If this method is called with a query which was created with .withScanForwards(false), then the rows will be returned
   * in descending order of sort key (i.e. reverse sequence). If the call includes an AsyncConsumerManager then the order
   * of processing is indeterminate, but if a (synchronous) ConsumerManager is passed then the objects will be observed
   * in the forwards or reverse sequence depending on whether .withScanForwards(true) or .withScanForwards(false)
   * was used respectively.
   * 
   * @param request   The request parameters.
   * 
   * @return If the invocation is asynchronous then a subscriber controller, you must call the start() method on this object and the stop()
   * method may be called for a graceful shutdown. If the invocation is synchronous then the return value is <code>null</code>.
   */
  @Nullable IAllegroQueryManager fetchPartitionObjects(FetchPartitionObjectsRequest request);

  /**
   * Fetch a page of objects from the given partition.
   * 
   * The returned IObjectPage allows the next and previous pages to be fetched.
   * 
   * Because this method is intended to be called in a paged context, the rows provided by the 
   * getData() method on the returned IObjectPage will always be in the forwards order (ascending order of sort key)
   * regardless of whether the query was specified with .withScanForwards(false) or .withBefore(String).
   * 
   * If such a query were made via fetchPartitionObjects(FetchPartitionObjectsRequest request) then the rows would
   * be returned in the reverse sequence.
   * 
   * @param query The query parameters for the objects required.
   * 
   * @return  A page of objects.
   */
  IObjectPage fetchPartitionObjectPage(PartitionQuery query);

  /**
   * 
   * @return The pod ID of the pod we are connected to.
   */
  PodId getPodId();
  
  /**
   * Create a new ApplicationObjectBuilder.
   * 
   * @return A new ApplicationObjectBuilder.
   */
  ApplicationObjectBuilder newApplicationObjectBuilder();
  
  /**
   * Create a new ApplicationObjectBuilder to create a new version of the given object.
   * 
   * @param existingObject An existing application object for which a new version is to be created.
   * 
   * @return A new ApplicationObjectBuilder to create a new version of the given object.
   */
  ApplicationObjectUpdater newApplicationObjectUpdater(IApplicationObjectPayload existingObject);
  
  /**
   * Create a new ApplicationObjectDeleter to delete the given object.
   * 
   * @param existingObject An existing application object which is to be deleted.
   * 
   * @return A new ApplicationObjectDeleter to delete the given object.
   */
  ApplicationObjectDeleter newApplicationObjectDeleter(IApplicationObjectPayload existingObject);
  
  /**
   * Fetch the meta-data for a sequence, or create it if it does not exist.
   * 
   * @param request The request parameters.
   * 
   * @return The sequence meta-data.
   */
  IPartition upsertPartition(UpsertPartitionRequest request);
//
//  /**
//   * Fetch an object by its absolute hash.
//   * 
//   * @param absoluteHash The hash of the required object.
//   * 
//   * @return The raw object.
//   * 
//   * @throws NotFoundException If the object does not exist. 
//   */
//  IFundamentalObject fetchAbsolute(Hash absoluteHash);
//  
//  /**
//   * Fetch an object by its absolute hash.
//   * 
//   * @param absoluteHash The hash of the required object.
//   * @param type         The type of the object.
//   * 
//   * @return The object, decrypted and unwrapped if we have appropriate credentials.
//   * 
//   * @throws NotFoundException      If the object does not exist.
//   * @throws IllegalStateException  If the object exists but is of some other type.
//   */
//  <T extends IEntity> T fetchAbsolute(Hash absoluteHash, Class<T> type);
//
//  /**
//   * Fetch the current version of an object by its base hash.
//   * 
//   * @param baseHash     The base hash of the required object.
//   * 
//   * @return The raw object.
//   * 
//   * @throws NotFoundException If the object does not exist. 
//   */
//  IFundamentalObject fetchCurrent(Hash baseHash);
//  
//  /**
//   * Fetch the current version of an object by its base hash.
//   * 
//   * @param baseHash     The base hash of the required object.
//   * @param type         The type of the object.
//   * 
//   * @return The object, decrypted and unwrapped if we have appropriate credentials.
//   * 
//   * @throws NotFoundException      If the object does not exist.
//   * @throws IllegalStateException  If the object exists but is of some other type.
//   */
//  <T extends IEntity> T fetchCurrent(Hash baseHash, Class<T> type);
//
//

  /**
   * Delete the given object.
   * 
   * The deletion may be logical or physical.
   * 
   * In the case of a logical delete, the object will be deleted from any current sequences of which it
   * is a member, and a delete record will be added to any absolute sequences of which it is a member
   * and as the latest version of the object.
   * 
   * When performing a fetch of a logically deleted object a DeletedException (HTTP status 410) will be
   * thrown, which is a sub-class of NotFoundException (HTTP status 404) so code which does not catch
   * DeletedException specifically will see a NotFoundException which is the result which would have 
   * occurred if the object had never existed.
   * 
   * In the case of a physical delete, all versions of the object are physically removed as are all
   * copies of the object on all sequences.
   * 
   * @param item          An existing object which is to be deleted.
   * @param deletionType  The type of deletion to be performed.
   */
  void delete(IApplicationObjectPayload item, DeletionType deletionType);

  /**
   * Upsert (insert or update as necessary) a feed with the given details. A feed is identified by a user ID and name tuple,
   * feeds can only be created with the userId of the creator.
   *  
   * This operation creates the feed if necessary and can also subscribe the feed to one or more partitions if it is not
   * already subscribed. This method is idempotent.
   *
   * e.g.
   * 
   * <code>
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
   * </code>
   *  
   * @param request The details of the feed to be created or returned.
   * 
   * @return The feed object.
   */
  IFeed upsertFeed(UpsertFeedRequest request);
  
  /**
   * Fetch objects from the given feed.
   * 
   * This method makes a synchronous or asynchronous request depending on whether the consumer provided by the request object
   * is a <code>ConsumerManager</code> or an <code>AsyncConsumerManager</code>
   * 
   * <h1>Synchronous Invocation</h1>
   * It is up to the server to decide how many objects to return, if there are more objects available than requested this does
   * NOT guarantee that the full number of objects requested will be returned.
   * 
   * e.g.
   * <code>
    allegroApi_.fetchFeedObjects(new FetchFeedObjectsRequest.Builder()
        .withName("myCalendarFeed")
        .withConsumerManager(new ConsumerManager.Builder()
            .withMaxItems(10)
            .withConsumer(Object.class, (object, trace) -&gt;
            {
              System.out.println(object);
            })
            .build())
        .build()
        );
   * </code>
   * 
   * In the case of a synchronous invocation this method returns <code>null</code>
   * 
   * <h1>Asynchronous Invocation</h1>
   * This invocation style uses two thread pools to asynchronously fetch messages.
   * 
   * The start() method must be called on the returned subscriber to begin processing messages.
   * 
   * e.g.
   * <code>
    IAllegroQueryManager subscriber = allegroApi_.fetchFeedObjects(new SubscribeFeedObjectsRequest.Builder()
        .withName("myCalendarFeed")
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
   * </code>
   * 
   * @param request The details of the request
   * 
   * @return If the invocation is asynchronous then a subscriber controller, you must call the start() method on this object and the stop()
   * method may be called for a graceful shutdown. If the invocation is synchronous then the return value is <code>null</code>. 
   * 
   */
  @Nullable IAllegroQueryManager fetchFeedObjects(FetchFeedObjectsRequest request);

  /**
   * The session token is required in a header called sessionToken for calls to public API methods and as a cookie called
   * skey in private methods intended for use by Symphony clients.
   * 
   * @return The session token.
   */
  String getSessionToken();

  /**
   * The key manager token is required in a cookie called kmsession for calls to private Key Manager methods.
   * 
   * THIS TOKEN SHOULD NEVER BE SENT TO ANY NON-KEY MANAGER ENDPOINTS, TO DO SO IS A SECURITY RISK.
   * 
   * @return The key manager token.
   */
  String getKeyManagerToken();

  /**
   * Fetch versions of the given logical object, by its baseHash.
   * 
   * @param request Request parameters.
   * 
   * @return an IAllegroQueryManager if this is an asynchronous request otherwise null.
   */
  @Nullable IAllegroQueryManager fetchObjectVersions(FetchObjectVersionsRequest request);

  /**
   * Create a new message feed.
   * 
   * There is a limit to the number of feeds which any one user may create.
   * 
   * @return The ID of the new feed.
   */
  FeedId createMessageFeed();

  /**
   * List all currently active message feeds for the calling user.
   * 
   * @return A list of FeedIds for all currently active message feeds for the calling user.
   */
  List<FeedId> listMessageFeeds();

  /**
   * Fetch messages from a message feed (Datafeed 2.0 feed).
   * 
   * @param request Request parameters.
   * 
   * @return The AckId which should be passed to the next request.
   */
  @Nullable AckId fetchFeedMessages(FetchFeedMessagesRequest request);
}
