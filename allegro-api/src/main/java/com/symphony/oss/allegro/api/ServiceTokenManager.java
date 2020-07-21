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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.canon.runtime.exception.ServerErrorException;
import com.symphony.oss.models.internal.pod.canon.ITokenHolder;
import com.symphony.oss.models.internal.pod.canon.ITokenResponse;
import com.symphony.oss.models.internal.pod.canon.PodInternalHttpModelClient;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

class ServiceTokenManager
{
  private static final Logger              log_             = LoggerFactory.getLogger(AllegroBaseApi.class);

  private static final long SESSION_EXPIRY_TIME = 60 * 60 * 1000; // 1 hour in millis
  
  private final PodInternalHttpModelClient podInternalApiClient_;
  private final CloseableHttpClient        httpClient_;
  private final IAuthHandler               authHandler_;

  private Map<String, String>              serviceTokenMap_ = new HashMap<>();
  private Map<String, Long>                expiryMap_       = new HashMap<>();
  private JwtParser                        jwtParser_       = Jwts.parserBuilder().build(); //new DefaultJwtParser();
  private String                           commonJwt_;
  private long                             commonJwtExpires_;
  private boolean                          commonJwtUnavailable_;
  
  ServiceTokenManager(PodInternalHttpModelClient podInternalApiClient, CloseableHttpClient httpClient, IAuthHandler authHandler)
  {
    podInternalApiClient_ = podInternalApiClient;
    httpClient_           = httpClient;
    authHandler_          = authHandler;
  }
  
  private Claims decodeTokenClaims(String token)
  {
    String[] splitToken = token.split("\\.");
    String unsignedToken = splitToken[0] + "." + splitToken[1] + ".";

    Jwt<?, ?> jwt = jwtParser_.parse(unsignedToken);
    Claims claims = (Claims) jwt.getBody();
    return claims;
  }

  synchronized String getServiceToken(String serviceId)
  {
    Long expiry = expiryMap_.get(serviceId);
    
    if(expiry == null || System.currentTimeMillis() > expiry)
    {
      List<ITokenHolder> tokenContainers = podInternalApiClient_.newSettingsWebApiV1TokensGetHttpRequestBuilder()
          .build()
          .execute(httpClient_)
          ;
        
      serviceTokenMap_.clear();
      
      long defaultExpiry = System.currentTimeMillis() + 1000 * 60 * 4; // 5 mins, less 1 min grace
      
      for(ITokenHolder tokenContainer : tokenContainers)
      {
        serviceTokenMap_.put(tokenContainer.getService(), tokenContainer.getToken());
        
        Claims claims = decodeTokenClaims(tokenContainer.getToken());
        
        if(claims.getExpiration() != null)
          expiryMap_.put(tokenContainer.getService(), claims.getExpiration().getTime() - 60000); // less 1 min grace
        else
          expiryMap_.put(tokenContainer.getService(), defaultExpiry);
      }
    }
    
    return serviceTokenMap_.get(serviceId);
  }
  
  synchronized String getCommonJwt()
  {
    if(!commonJwtUnavailable_)
    {
      try
      {
        if(System.currentTimeMillis() > commonJwtExpires_)
        {
          ITokenResponse tokenResponse = podInternalApiClient_.newLoginIdmTokensPostHttpRequestBuilder()
            .build()
            .execute(httpClient_)
            ;
          
          int expiresIn = tokenResponse.getExpiresIn() == null ? 300 : tokenResponse.getExpiresIn();
          
          commonJwtExpires_ = (expiresIn - 30) * 1000 + System.currentTimeMillis();
          commonJwt_ = tokenResponse.getAccessToken();
        }
        
        return commonJwt_;
      }
      catch(ServerErrorException e)
      {
        log_.warn("Common JWT unavailable from pod.");
        commonJwtUnavailable_ = true;
      }
    }
    return getAuthToken();
    //return getServiceToken("datafeed2");
  }
  
  synchronized String getAuthToken()
  {
    if(System.currentTimeMillis() > commonJwtExpires_)
    {
      authHandler_.authenticate(true, false);
      commonJwtExpires_ = SESSION_EXPIRY_TIME - 30000 + System.currentTimeMillis();
    }
    
    return authHandler_.getSessionToken();
  }
}
