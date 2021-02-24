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

package com.symphony.oss.allegro2.api;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.impl.client.CloseableHttpClient;
import org.symphonyoss.symphony.messageml.exceptions.InvalidInputException;
import org.symphonyoss.symphony.messageml.util.IDataProvider;
import org.symphonyoss.symphony.messageml.util.IUserPresentation;

import com.google.common.collect.ImmutableSet;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.UserId;
import com.symphony.oss.models.internal.pod.canon.IPodInfo;
import com.symphony.oss.models.pod.canon.IUserV2;
import com.symphony.oss.models.pod.canon.PodHttpModelClient;

class AllegroDataProvider implements IDataProvider
{
  private static final Set<String> STANDARD_URI_SCHEMES = ImmutableSet.of("http", "https");

  
  private final CloseableHttpClient        httpclient_;
  private final PodHttpModelClient         podApiClient_;
  private final IPodInfo podInfo_;
  private final String sessionToken_;
  
  
  private Set<String> getSupportedUriSchemes_;

  AllegroDataProvider(CloseableHttpClient httpclient, PodHttpModelClient podApiClient, IPodInfo podInfo, String sessionToken)
  {
    httpclient_   = httpclient;
    podApiClient_ = podApiClient;
    podInfo_      = podInfo;
    sessionToken_ = sessionToken;
  }

  @Override
  public IUserPresentation getUserPresentation(String emailAddress)
  {
    try
    {
      IUserV2 userInfo = podApiClient_.newV2UserGetHttpRequestBuilder()
          .withSessionToken(sessionToken_)
          .withEmail(emailAddress)
          .withLocal(true)
          .build()
          .execute(httpclient_)
          ;
        
        return new UserPresentation(userInfo.getId(), userInfo.getUsername(), userInfo.getDisplayName(), userInfo.getEmailAddress());
    }
    catch(RuntimeException e)
    {
      IUserV2 userInfo = podApiClient_.newV2UserGetHttpRequestBuilder()
          .withSessionToken(sessionToken_)
          .withEmail(emailAddress)
          .withLocal(false)
          .build()
          .execute(httpclient_)
          ;
        
        return new UserPresentation(userInfo.getId(), userInfo.getUsername(), userInfo.getDisplayName(), userInfo.getEmailAddress());
    }
  }

  @Override
  public IUserPresentation getUserPresentation(Long uid)
  {
    boolean local = UserId.extractPodId(uid) == podInfo_.getPodId();
    
    IUserV2 userInfo = podApiClient_.newV2UserGetHttpRequestBuilder()
      .withSessionToken(sessionToken_)
      .withUid(PodAndUserId.newBuilder().build(uid))
      .withLocal(local)
      .build()
      .execute(httpclient_)
      ;
    
    return new UserPresentation(userInfo.getId(), userInfo.getUsername(), userInfo.getDisplayName(), userInfo.getEmailAddress());
  }

  @Override
  public void validateURI(URI uri) throws InvalidInputException
  {
    if (!STANDARD_URI_SCHEMES.contains(uri.getScheme().toLowerCase())) {
      if(!getSupportedUriSchemes().contains(uri.getScheme())) {
        throw new InvalidInputException(
            "URI scheme \"" + uri.getScheme() + "\" is not supported by the pod.");
      }
    }
  }

  private Set<String> getSupportedUriSchemes()
  {
    if(getSupportedUriSchemes_ == null)
    {
      // TODO: fix canon to be able to return list of string
//      List<IUserV2> result = podApiClient_.newV1AdminSystemProtocolsListGetHttpRequestBuilder()
//        .build()
//        .execute(httpclient_);
      
      getSupportedUriSchemes_ = new HashSet<>();
    }
    return getSupportedUriSchemes_;
  }

}
