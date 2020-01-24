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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;

import com.symphony.oss.models.internal.pod.canon.ITokenHolder;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.impl.DefaultJwtParser;

class ServiceTokenManager
{
  private final PodInternalHttpModelClient podInternalApiClient_;
  private final CloseableHttpClient        httpClient_;

  private Map<String, String>              serviceTokenMap_ = new HashMap<>();
//  private Map<String, Jwt<Claims>>              serviceJwtMap_   = new HashMap<>();
  private JwtParser                        jwtParser_       = new DefaultJwtParser();
  
  ServiceTokenManager(PodInternalHttpModelClient podInternalApiClient, CloseableHttpClient httpClient)
  {
    podInternalApiClient_ = podInternalApiClient;
    httpClient_ = httpClient;
    
    List<ITokenHolder> tokenContainers = podInternalApiClient_.newSettingsWebApiV1TokensGetHttpRequestBuilder()
        .build()
        .execute(httpClient_)
        ;
      
      serviceTokenMap_.clear();
      
      for(ITokenHolder tokenContainer : tokenContainers)
      {
        serviceTokenMap_.put(tokenContainer.getService(), tokenContainer.getToken());
        
//        Claims claims = decodeTokenClaims(tokenContainer.getToken());
//        
//        System.err.print(claims.getExpiration());
      }
  }
  
  private Claims decodeTokenClaims(String token)
  {
    String[] splitToken = token.split("\\.");
    String unsignedToken = splitToken[0] + "." + splitToken[1] + ".";

    DefaultJwtParser parser = new DefaultJwtParser();
    Jwt<?, ?> jwt = parser.parse(unsignedToken);
    Claims claims = (Claims) jwt.getBody();
    return claims;
  }

  String getServiceToken(String serviceId)
  {
    return serviceTokenMap_.get(serviceId);
  }
}
