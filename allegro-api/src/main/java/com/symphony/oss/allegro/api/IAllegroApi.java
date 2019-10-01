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
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.symphonyoss.s2.canon.runtime.IEntity;
import org.symphonyoss.s2.canon.runtime.exception.NotFoundException;
import org.symphonyoss.s2.common.fluent.IFluent;
import org.symphonyoss.s2.common.hash.Hash;

import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectBuilder;
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectUpdater;
import com.symphony.oss.models.allegro.canon.facade.ChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.facade.ISocialMessage;
import com.symphony.oss.models.chat.canon.facade.ThreadId;
import com.symphony.oss.models.fundamental.canon.facade.IApplicationObject;
import com.symphony.oss.models.fundamental.canon.facade.IFundamentalId;
import com.symphony.oss.models.fundamental.canon.facade.IFundamentalObject;
import com.symphony.oss.models.fundamental.canon.facade.INotification;
import com.symphony.oss.models.fundamental.canon.facade.PodAndUserId;
import com.symphony.oss.models.fundamental.canon.facade.PodId;
import com.symphony.oss.models.fundmental.canon.DeletionType;
import com.symphony.oss.models.fundmental.canon.IPageOfFundamentalObject;
import com.symphony.oss.models.fundmental.canon.ISequence;
import com.symphony.oss.models.pod.canon.IUserV2;
import com.symphony.oss.models.system.canon.IFeed;
import com.symphony.oss.models.system.canon.facade.IFeedMessage;

/**
 * The public interface of the Allegro API.
 * 
 * @author Bruce Skingle
 *
 */
public interface IAllegroApi extends IFluent<IAllegroApi>
{
  public static final String SYMPHONY_DEV_QA_ROOT_CERT         = "/certs/symphony/devQaRoot.pem";
  public static final String SYMPHONY_DEV_QA_INTERMEDIATE_CERT = "/certs/symphony/devQaIntermediate.pem";
  public static final String SYMPHONY_DEV_CERT                 = "/certs/symphony/dev.pem";
  
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
   * This implementation retrieves messages from the object store.
   * 
   * @param request   A request object containing the threadId and other parameters.
   * @param consumer  A consumer of decrypted messages. 
   * 
   */
  void fetchRecentMessages(FetchRecentMessagesRequest request, Consumer<IChatMessage> consumer);

  /**
   * Fetch recent messages from a thread (conversation).
   * 
   * This implementation retrieves messages from the pod.
   * 
   * @param request   A request object containing the threadId and other parameters.
   * @param consumer  A consumer of decrypted messages. 
   * 
   */
  void fetchRecentMessagesFromPod(FetchRecentMessagesRequest request, Consumer<IChatMessage> consumer);
  
  /**
   * Decrypt the given SocialMessage.
   * 
   * @param message A SocialMessage including encrypted fields.
   * 
   * @return The decrypted payload of the given message.
   */
  IReceivedChatMessage decrypt(ISocialMessage message);

  /**
   * Send the given chat message.
   * 
   * @param chatMessage A message to be sent.
   */
  void sendMessage(IChatMessage chatMessage);

//  /**
//   * Encrypt the given ApplicationObject with the content key for the given thread.
//   * 
//   * Note that this is a client only operation, the encrypted object is NOT stored as a result of calling this method.
//   * 
//   * @param threadId            The threadId of the thread (conversation) whose content key should be used for the encryption
//   * @param applicationPayload  The payload to be encrypted.
//   * @param baseHash            An optional hash being the ID of the initial version of a versioned object.
//   * @param prevHash            An optional hash being the ID of the previous version from which this object was created.
//   * @param sequenceHashes      An optional list of sequences to which this object should be added.
//   * 
//   * @return  An encrypted FundamentalObject containing the given payload.
//   */
//  IFundamentalObject encrypt(ThreadId threadId, IApplicationObject applicationPayload, @Nullable Hash baseHash, @Nullable Hash prevHash, @Nullable List<Hash> sequenceHashes);
//
//
//
//  /**
//   * Encrypt and store the given ApplicationObject with the content key for the given thread.
//   * 
//   * @param threadId            The threadId of the thread (conversation) whose content key should be used for the encryption
//   * @param applicationPayload  The payload to be encrypted.
//   * @param baseHash            An optional hash being the ID of the initial version of a versioned object.
//   * @param prevHash            An optional hash being the ID of the previous version from which this object was created.
//   * @param sequenceHashes      An optional list of sequences to which this object should be added.
//   * 
//   * @return  An encrypted FundamentalObject containing the given payload.
//   */
//  IFundamentalObject store(ThreadId threadId, IApplicationObject applicationPayload, @Nullable Hash baseHash, @Nullable Hash prevHash, @Nullable List<Hash> sequenceHashes);
//
//  /**
//   * Encrypt and store the given ApplicationObject with the content key for the given thread.
//   * 
//   * @param threadId            The threadId of the thread (conversation) whose content key should be used for the encryption
//   * @param applicationPayload  The payload to be encrypted.
//   * @param baseHash            An optional hash being the ID of the initial version of a versioned object.
//   * @param prevHash            An optional hash being the ID of the previous version from which this object was created.
//   * @param sequenceId          An optional list of sequences to which this object should be added.
//   * 
//   * @return  An encrypted FundamentalObject containing the given payload.
//   */
//  IFundamentalObject store(ThreadId threadId, IApplicationObject applicationPayload, Hash baseHash, Hash prevHash,
//      IFundamentalId ...sequenceId);
  
  /**
   * 
   * @return The principalHash (globally unique ID) of the user we have authenticated as.
   */
  Hash getPrincipalBaseHash();

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
   * @param object A FundamentalObject to be stored.
   */
  void store(IFundamentalObject object);

  /**
   * Store the given ID.
   * 
   * @param id A IFundamentalId to be stored.
   * 
   * @return The Fundamental Object wrapping this ID.
   */
  IFundamentalObject store(IFundamentalId id);

  /**
   * Fetch a page of items from the given sequence.
   * 
   * @param sequenceId  The Hash identifier for a sequence.
   * @param limit       An optional limit for the maximum number of items to return.
   * @param after       A cursor indicating where in the sequence to start for continuation requests.
   *  
   * @return  A page of objects from the given sequence.
   */
  IPageOfFundamentalObject fetchSequencePage(IFundamentalId sequenceId, @Nullable Integer limit, String after);

  /**
   * Open (deserialize and decrypt if necessary) the given object.
   * 
   * @param item A FundamentalObject.
   * 
   * @return The typed contents of the given object.
   */
  IEntity open(IFundamentalObject item);

  /**
   * Open (deserialize and decrypt if necessary) the given object, storing wrapped keys from the key manager if necessary.
   * 
   * @param item A FundamentalObject.
   * @param threadId The thread ID of the message thread to which this object belongs.
   * 
   * @return The typed contents of the given object.
   */
  IEntity open(IFundamentalObject item, ThreadId threadId);

  /**
   * 
   * @return A new builder for a chat message.
   */
  ChatMessage.Builder newChatMessageBuilder();

  /**
   * Fetch objects from a sequence.
   * 
   * @param request   The request parameters.
   * @param consumer  A consumer to receive returned objects.
   */
  void fetchSequence(FetchSequenceRequest request, Consumer<IFundamentalObject> consumer);

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
  ApplicationObjectUpdater newApplicationObjectUpdater(IApplicationObject existingObject);

  /**
   * Fetch the meta-data for a sequence, or create it if it does not exist.
   * 
   * @param request The request parameters.
   * 
   * @return The sequence meta-data.
   */
  ISequence fetchOrCreateSequenceMetaData(FetchOrCreateSequenceMetaDataRequest request);
  
  /**
   * Fetch the meta-data for a sequence.
   * 
   * @param request The request parameters.
   * 
   * @return The sequence meta-data.
   * 
   * @throws NotFoundException If the sequence does not exist. 
   */
  ISequence fetchSequenceMetaData(FetchSequenceMetaDataRequest request);

  /**
   * Fetch an object by its absolute hash.
   * 
   * @param absoluteHash The hash of the required object.
   * 
   * @return The raw object.
   * 
   * @throws NotFoundException If the object does not exist. 
   */
  IFundamentalObject fetchAbsolute(Hash absoluteHash);
  
  /**
   * Fetch an object by its absolute hash.
   * 
   * @param absoluteHash The hash of the required object.
   * @param type         The type of the object.
   * 
   * @return The object, decrypted and unwrapped if we have appropriate credentials.
   * 
   * @throws NotFoundException      If the object does not exist.
   * @throws IllegalStateException  If the object exists but is of some other type.
   */
  <T extends IEntity> T fetchAbsolute(Hash absoluteHash, Class<T> type);

  /**
   * Fetch the current version of an object by its base hash.
   * 
   * @param baseHash     The base hash of the required object.
   * 
   * @return The raw object.
   * 
   * @throws NotFoundException If the object does not exist. 
   */
  IFundamentalObject fetchCurrent(Hash baseHash);
  
  /**
   * Fetch the current version of an object by its base hash.
   * 
   * @param baseHash     The base hash of the required object.
   * @param type         The type of the object.
   * 
   * @return The object, decrypted and unwrapped if we have appropriate credentials.
   * 
   * @throws NotFoundException      If the object does not exist.
   * @throws IllegalStateException  If the object exists but is of some other type.
   */
  <T extends IEntity> T fetchCurrent(Hash baseHash, Class<T> type);

  /**
   * Save the credential for the current session as a cloud service provider secret.
   */
  void storeCredential();

  IFeed upsertFeed(UpsertSmsGatewayRequest request);

  IFeed upsertFeed(UpsertFeedRequest request);

  List<IFeedMessage> fetchFeedMessages(FetchFeedMessagesRequest request);

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
  void delete(IFundamentalObject item, DeletionType deletionType);
}
