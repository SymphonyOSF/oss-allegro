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

import java.security.cert.X509Certificate;
import java.util.List;

import javax.annotation.Nullable;

import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.commons.immutable.ImmutableByteArray;
import com.symphony.oss.models.allegro.canon.facade.ChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.core.canon.facade.IApplicationRecord;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.internal.pod.canon.AckId;
import com.symphony.oss.models.internal.pod.canon.FeedId;
import com.symphony.oss.models.pod.canon.IStreamAttributes;
import com.symphony.oss.models.pod.canon.IUserV2;
import com.symphony.oss.models.pod.canon.IV2UserList;

/**
 * The public interface of the Allegro Pod API.
 *
 * This API supports encryption and decryption capabilities and requires authentication to a pod.
 * 
 * @author Bruce Skingle
 *
 */
public interface IAllegro2Api  extends IAllegro2Decryptor, AutoCloseable
{
  @Override
  void close();

  /**
   * Return the user ID of the user we have authenticated as.
   * 
   * @return The user ID of the user we have authenticated as.
   */
  PodAndUserId getUserId();
  
  /**
   * Return the authorization token for calls to API endpoints.
   * 
   * @return The authorization token for calls to API endpoints.
   */
  String getApiAuthorizationToken();

  /**
   * The session token is required in a header called sessionToken for calls to public API methods and as a cookie called
   * skey in private methods intended for use by Symphony clients.
   *
   * @return The session token.
   */
  String getSessionToken();
  
  /**
   * Force authentication.
   */
  void authenticate();

  /**
   * Return info for the session with the pod.
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
   * Create a new ApplicationRecordBuilder.
   * 
   * This can be used to build an encrypted payload which can be stored in a database.
   * 
   * @return A new ApplicationRecordBuilder.
   */
  ApplicationRecordBuilder newApplicationRecordBuilder();

  /**
   * Encrypt the given payload.
   * 
   * @param builder
   */
  void encrypt(EncryptablePayloadBuilder<?, ?> builder);

  /**
   * Decrypt the given payload.
   * 
   * @param threadId          The threadId whose content key is used to encrypt the payload. 
   * @param rotationId        The rotation in force when the payload was encrypted
   * @param encryptedPayload  The payload.
   * 
   * @return The decrypted payload.
   */
  ImmutableByteArray decrypt(ThreadId threadId, RotationId rotationId, EncryptedData encryptedPayload);

  /**
   * Deserialize and decrypt the given object.
   * 
   * @param jsonObject An EncryptedApplicationRecord object.
   * 
   * @return The decrypted object.
   */
  IApplicationRecord decrypt(String jsonObject);

  /**
   * Parse SocialMessage text. For MessageMLV2 messages, returns the PresentationML content. For legacy messages, parses
   * the Markdown content and JSON entities and returns their PresentationML representation.
   * 
   * @param message   A LiveCurrentMessage with encrypted payload.
   * 
   * @return          A ReceivedChatMessage with the decrypted payload.
   */
  @Override
  IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message);

//  /**
//   * Deserialize and decrypt the given object.
//   * 
//   * @param <T> Type of the required object payload.
//   * 
//   * @param storedApplicationObject An encrypted object.
//   * @param type                    Type of the required object payload.
//   * 
//   * @return The decrypted object.
//   * 
//   * @throws IllegalStateException If the decrypted payload is not an instance of the required type.
//   */
//  public <T extends IApplicationObjectPayload> T decryptObject(IEncryptedApplicationPayload storedApplicationObject, Class<T> type);


  /**
   * Fetch information about a user given a user (login) name.
   * 
   * @param userName  The userName with which the required user logs in.
   * 
   * @return The user object for the required user.
   * 
   * @throws NotFoundException If the given userName cannot be found.
   */
  IUserV2 fetchUserByName(String userName) throws NotFoundException;

  /**
   * Fetch information about one or more users given a user (login) name.
   * 
   * @param userNames  The userName with which the required users log in.
   * 
   * @return A list of responses and errors.
   */
  IV2UserList fetchUsersByName(String... userNames);

  /**
   * Fetch information about streams (conversations or threads) the caller is a member of.
   * 
   * @param fetchStreamsRequest Request parameters.
   * 
   * @return A list of objects describing streams of which the caller is a member.
   */
  List<IStreamAttributes> fetchStreams(FetchStreamsRequest fetchStreamsRequest);

  /**
   * Fetch information about a user given an external user ID.
   * 
   * @param userId  The external user ID of the required user.
   * 
   * @return The user object for the required user.
   * 
   * @throws NotFoundException If the given userName cannot be found.
   */
  IUserV2 fetchUserById(PodAndUserId userId) throws NotFoundException;

  /**
   * Return the ModelRegistry used by Allegro.
   * 
   * @return the ModelRegistry used by Allegro.
   */
  ModelRegistry getModelRegistry();

  /**
   * Return a new StoredRecordConsumerManager builder.
   * 
   * @return a new StoredRecordConsumerManager builder.
   */
  AllegroConsumerManager.AbstractBuilder<?,?> newConsumerManagerBuilder();
}
