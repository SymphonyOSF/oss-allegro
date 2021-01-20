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

package com.symphony.oss.allegro.api;

import java.security.cert.X509Certificate;
import java.util.List;

import com.symphony.oss.allegro.api.request.FetchFeedMessagesRequest;
import com.symphony.oss.allegro.api.request.FetchRecentMessagesRequest;
import com.symphony.oss.allegro.api.request.FetchStreamsRequest;
import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.models.allegro.canon.ChatMessageEntity.Builder;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.internal.pod.canon.AckId;
import com.symphony.oss.models.internal.pod.canon.FeedId;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.pod.canon.IStreamAttributes;
import com.symphony.oss.models.pod.canon.IUserV2;
import com.symphony.oss.models.pod.canon.IV2UserList;

public interface IAllegroDelegatedPodApi extends IAllegroPodApi
{
  IAllegroPodApi getAllegroPodApi();
  
  @Override
  default IApplicationObjectPayload decryptObject(IEncryptedApplicationPayload storedApplicationObject)
  {
    return getAllegroPodApi().decryptObject(storedApplicationObject);
  }

  @Override
  default <T extends IApplicationObjectPayload> T decryptObject(IEncryptedApplicationPayload storedApplicationObject,
      Class<T> type)
  {
    return getAllegroPodApi().decryptObject(storedApplicationObject, type);
  }

  @Override
  default String getSessionToken()
  {
    return getAllegroPodApi().getSessionToken();
  }

  @Override
  default void authenticate()
  {
    getAllegroPodApi().authenticate();
  }

  @Override
  default IUserV2 getSessioninfo()
  {
    return getAllegroPodApi().getSessioninfo();
  }

  @Override
  default IUserV2 getUserInfo()
  {
    return getAllegroPodApi().getUserInfo();
  }

  @Override
  default String getMessage(String messageId)
  {
    return getAllegroPodApi().getMessage(messageId);
  }

  @Override
  default void fetchRecentMessagesFromPod(FetchRecentMessagesRequest request)
  {
    getAllegroPodApi().fetchRecentMessagesFromPod(request);
  }

  @Override
  default void sendMessage(IChatMessage chatMessage)
  {
    getAllegroPodApi().sendMessage(chatMessage);
  }

  @Override
  default X509Certificate getPodCert()
  {
    return getAllegroPodApi().getPodCert();
  }

  @Override
  default Builder newChatMessageBuilder()
  {
    return getAllegroPodApi().newChatMessageBuilder();
  }

  @Override
  default PodId getPodId()
  {
    return getAllegroPodApi().getPodId();
  }

  @Override
  default String getKeyManagerToken()
  {
    return getAllegroPodApi().getKeyManagerToken();
  }

  @Override
  default FeedId createMessageFeed()
  {
    return getAllegroPodApi().createMessageFeed();
  }

  @Override
  default List<FeedId> listMessageFeeds()
  {
    return getAllegroPodApi().listMessageFeeds();
  }

  @Override
  default AckId fetchFeedMessages(FetchFeedMessagesRequest request)
  {
    return getAllegroPodApi().fetchFeedMessages(request);
  }

  @Override
  default EncryptedApplicationPayloadBuilder newEncryptedApplicationPayloadBuilder()
  {
    return getAllegroPodApi().newEncryptedApplicationPayloadBuilder();
  }
  
  @Override
  default ApplicationRecordBuilder newApplicationRecordBuilder()
  {
    return getAllegroPodApi().newApplicationRecordBuilder();
  }

  @Override
  default IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message)
  {
    return getAllegroPodApi().decryptChatMessage(message);
  }

  @Override
  default IUserV2 fetchUserByName(String userName) throws NotFoundException
  {
    return getAllegroPodApi().fetchUserByName(userName);
  }

  @Override
  default IV2UserList fetchUsersByName(String... userNames)
  {
    return getAllegroPodApi().fetchUsersByName(userNames);
  }

  @Override
  default List<IStreamAttributes> fetchStreams(FetchStreamsRequest fetchStreamsRequest)
  {
    return getAllegroPodApi().fetchStreams(fetchStreamsRequest);
  }

  @Override
  default IUserV2 fetchUserById(PodAndUserId userId) throws NotFoundException
  {
    return getAllegroPodApi().fetchUserById(userId);
  }
}
