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

package com.symphony.oss.allegro.api.auth;

import java.net.URL;
import java.security.Key;
import java.security.PrivateKey;

import javax.annotation.Nullable;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.symphonyoss.s2.canon.runtime.IModelRegistry;
import org.symphonyoss.s2.canon.runtime.ModelRegistry;
import org.symphonyoss.s2.canon.runtime.exception.NotAuthenticatedException;
import org.symphonyoss.s2.canon.runtime.http.client.IJwtAuthenticationProvider;
import org.symphonyoss.s2.canon.runtime.jjwt.Rs512JwtGenerator;

import com.symphony.oss.models.auth.canon.AuthHttpModelClient;
import com.symphony.oss.models.auth.canon.AuthModel;
import com.symphony.oss.models.auth.canon.INamedToken;
import com.symphony.oss.models.auth.canon.IToken;
import com.symphony.oss.models.auth.canon.PubkeyAuthenticatePostHttpRequest;
import com.symphony.oss.models.auth.canon.PubkeyAuthenticatePostHttpRequestBuilder;
import com.symphony.oss.models.auth.canon.Token;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;

/**
 * Handler for the bot RSA authentication mechanism.
 * 
 * This class attempts to extract the userId from the received token, if we can do this and if the returned userId is not an internal one
 * (i.e. we are not on one of the special pods with a different internal podId) we can avoid making calls to podInfo and accountInfo.
 * 
 * @author Bruce Skingle
 *
 */
public class AuthHandler
{
  private final CloseableHttpClient        httpClient_;
  private final CookieStore                cookieStore_;
  private final PrivateKey                 rsaCredential_;
  private final IModelRegistry             modelRegistry_;
  private final IJwtAuthenticationProvider authProvider_;
  private AuthHttpModelClient              keyManagerClient_;
  private final AuthHttpModelClient        podClient_;

  private INamedToken                      keyManagerToken_;
  private INamedToken                      sessionToken_;
  private String                           domain_;
  

  private PodAndUserId userId_;
  
  public AuthHandler(CloseableHttpClient httpClient, CookieStore cookieStore, URL podUrl, PrivateKey rsaCredential, String serviceAccountName)
  {
    httpClient_     = httpClient;
    cookieStore_    = cookieStore;
    rsaCredential_  = rsaCredential;
    modelRegistry_  = new ModelRegistry().withFactories(AuthModel.FACTORIES);
    
    authProvider_   = new Rs512JwtGenerator(rsaCredential_)
        .withSubject(serviceAccountName)
        .withTTL(300000)  // 5 minutes, this is the max allowed by Symphony RSA authentication.
        ;
    
    domain_ = podUrl.getHost();
    
    podClient_ = new AuthHttpModelClient(
        modelRegistry_,
        podUrl, "/login", null);
  }
  
  public void authenticate(boolean authSession, boolean authKeyManager)
  {
    String jwtToken = authProvider_.createJwt();
    IToken token;

    token = new Token.Builder()
      .withToken(jwtToken)
      .build();
    
    if(authSession)
    {
      sessionToken_    = authenticate(podClient_, token);
      addCookie("skey", sessionToken_);
      
      if(userId_ == null)
      {
        setUserId(sessionToken_);
      }
    }
    
    if(authKeyManager)
    {
      keyManagerToken_ = authenticate(keyManagerClient_, token);
      addCookie("kmsession", keyManagerToken_);
      
      if(userId_ == null)
      {
        setUserId(sessionToken_);
      }
    }
  }

  private void setUserId(INamedToken token)
  {
    try
    {
      Jwts.parser()
          .setSigningKeyResolver(new SigningKeyResolver())
          .parseClaimsJws(token.getToken());
    }
    catch(NotAuthenticatedException e)
    {
      // expected
    }
  }

  public String getKeyManagerToken()
  {
    authenticateIfNecessary();
    return keyManagerToken_.getToken();
  }

  public String getSessionToken()
  {
    authenticateIfNecessary();
    return sessionToken_.getToken();
  }

  private void authenticateIfNecessary()
  {
    if(sessionToken_ == null)
      authenticate(true, true);
  }

  private void addCookie(String name, INamedToken sessionToken)
  {

    BasicClientCookie cookie = new BasicClientCookie(name, sessionToken.getToken());
    
    cookie.setDomain(domain_);
    
    cookieStore_.addCookie(cookie);
  }

  private INamedToken authenticate(AuthHttpModelClient client, IToken token)
  {
    PubkeyAuthenticatePostHttpRequest request = new PubkeyAuthenticatePostHttpRequestBuilder(client)
        .withCanonPayload(token)
        .build();
      
      return request.execute(httpClient_);
  }

  /**
   * 
   * @return The callers external userId if this can be determined, otherwise null.
   * 
   */
  public @Nullable PodAndUserId getUserId()
  {
    if(userId_ != null && userId_.getPodId().getValue() != 1)
      return userId_;
    
    return null;
  }

  public void setKeyManagerUrl(String keyManagerUrl)
  {
    keyManagerClient_ = new AuthHttpModelClient(
        modelRegistry_,
        keyManagerUrl, null, null);
  }

  class SigningKeyResolver extends SigningKeyResolverAdapter
  {

    @Override
    public Key resolveSigningKey(@SuppressWarnings("rawtypes") JwsHeader header, Claims claims)
    {
      userId_ = PodAndUserId.newBuilder().build(getUserId(claims));
       
      throw new NotAuthenticatedException();
    }

    private Long getUserId(Claims claims)
    {
      Object userIdObject = claims.get("userId");
      
      if(userIdObject instanceof Long)
        return (Long) userIdObject;
      
      return Long.parseLong(userIdObject.toString());
    }
  }
}
