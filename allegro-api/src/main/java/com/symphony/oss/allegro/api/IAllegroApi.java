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

import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectBuilder;
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectUpdater;
import com.symphony.oss.allegro.api.AllegroApi.EncryptedApplicationPayloadAndHeaderBuilder;
import com.symphony.oss.allegro.api.AllegroApi.EncryptedApplicationPayloadBuilder;
import com.symphony.oss.allegro.api.request.FetchFeedMessagesRequest;
import com.symphony.oss.allegro.api.request.FetchRecentMessagesRequest;
import com.symphony.oss.models.allegro.canon.facade.ChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.internal.pod.canon.AckId;
import com.symphony.oss.models.internal.pod.canon.FeedId;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.pod.canon.IUserV2;

/**
 * The public interface of the Allegro API.
 * 
 * @author Bruce Skingle
 *
 */
public interface IAllegroApi extends IAllegroMultiTenantApi
{
  /** Resource path for Symphony dev/QA root certificate */
  static final String SYMPHONY_DEV_QA_ROOT_CERT         = "/certs/symphony/devQaRoot.pem";
  /** Resource path for Symphony dev/QA intermediate certificate */
  static final String SYMPHONY_DEV_QA_INTERMEDIATE_CERT = "/certs/symphony/devQaIntermediate.pem";
  /** Resource path for Symphony dev certificate */
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
   * 
   * @return A new builder for a chat message.
   */
  ChatMessage.Builder newChatMessageBuilder();

  /**
   * 
   * @return The pod ID of the pod we are connected to.
   */
  PodId getPodId();

  /**
   * The key manager token is required in a cookie called kmsession for calls to private Key Manager methods.
   * 
   * THIS TOKEN SHOULD NEVER BE SENT TO ANY NON-KEY MANAGER ENDPOINTS, TO DO SO IS A SECURITY RISK.
   * 
   * @return The key manager token.
   */
  String getKeyManagerToken();

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
  
  /**
   * Create a new EncryptedApplicationPayloadBuilder.
   * 
   * This can be used to build an encrypted payload which can be sent to a server end point to be stored in the object store 
   * or elsewhere
   * 
   * @return A new EncryptedApplicationPayloadBuilder.
   */
  EncryptedApplicationPayloadBuilder newEncryptedApplicationPayloadBuilder();
  
  /**
   * Create a new EncryptedApplicationPayloadAndHeaderBuilder.
   * 
   * This can be used to build an encrypted payload and header which can be sent to a server end point to be stored in the object store 
   * or elsewhere
   * 
   * @return A new EncryptedApplicationPayloadAndHeaderBuilder.
   */
  EncryptedApplicationPayloadAndHeaderBuilder newEncryptedApplicationPayloadAndHeaderBuilder();
  
  /**
   * Create a new ApplicationObjectBuilder.
   * 
   * @return A new ApplicationObjectBuilder.
   */
  ApplicationObjectBuilder newApplicationObjectBuilder();
  
  /**
   * Create a new ApplicationObjectBuilder to create a new version of the given object.
   * 
   * When an application object payload is returned from fetch operations it has a reference to the IStoredApplicationObject
   * which contains it and this can be retrieved by calling the <code>getStoredApplicationObject()</code>
   * method. In these cases (such as when writing a Consumer method) you can generate an updater from the
   * application object payload directly by calling
   * <code>newApplicationObjectUpdater(applicationObjectPayload.getStoredApplicationObject())</code>.
   * 
   * In cases where the application object was created (rather than having been retrieved from the object store)
   * this will not work, since the ApplicationObjectPayload must be created before the StoredApplicationObject.
   * In order to update an object from a created ApplicationObjectPayload it is necessary to retain a separate
   * reference to the StoredApplicationObject which is returned by the
   * <code>store(IAbstractStoredApplicationObject object)</code> method.
   * 
   * @param existingObject An existing application object for which a new version is to be created.
   * 
   * @return A new ApplicationObjectBuilder to create a new version of the given object.
   */
  ApplicationObjectUpdater newApplicationObjectUpdater(IStoredApplicationObject existingObject);
  
  /**
   * Create a new ApplicationObjectBuilder to create a new version of the given object.
   * 
   * This method only works with an IApplicationObjectPayload which was retrieved from the object store, if it
   * is passed a locally created IApplicationObjectPayload then a <code>NullPointerException</code> will be thrown.
   * 
   * @param existingObject An existing application object for which a new version is to be created.
   * 
   * @deprecated Use <code>newApplicationObjectUpdater(IStoredApplicationObject existingObject)</code> instead.
   * 
   * @return A new ApplicationObjectBuilder to create a new version of the given object.
   */
  @Deprecated
  default ApplicationObjectUpdater newApplicationObjectUpdater(IApplicationObjectPayload existingObject)
  {
    return newApplicationObjectUpdater(existingObject.getStoredApplicationObject());
  }

  /**
   * Create an IChatMessage from the given ILiveCurrentMessage, if the message
   * is an ISocialMessage then the message payload is decrypted.
   * 
   * @param message An ILiveCurrentMessage.
   * 
   * @return An IChatMessage representing the given message.
   */
  public IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message);

  /**
   * Open (deserialize and decrypt) the given object.
   * 
   * @param storedApplicationObject An encrypted object.
   * 
   * @return The decrypted object.
   */
  public IApplicationObjectPayload decryptObject(IStoredApplicationObject storedApplicationObject);

  /**
   * Open (deserialize and decrypt) the given object.
   * 
   * @param <T> Type of the required object payload.
   * 
   * @param storedApplicationObject An encrypted object.
   * @param type                    Type of the required object payload.
   * 
   * @return The decrypted object.
   * 
   * @throws IllegalStateException If the decrypted payload is not an instance of the required type.
   */
  public <T extends IApplicationObjectPayload> T decryptObject(IStoredApplicationObject storedApplicationObject, Class<T> type);
}
