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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.http.client.IJwtAuthenticationProvider;
import com.symphony.oss.canon.runtime.http.client.ResponseHandlerAction;
import com.symphony.oss.canon.runtime.jjwt.Rs512JwtGenerator;
import com.symphony.oss.models.auth.canon.AuthHttpModelClient;
import com.symphony.oss.models.auth.canon.AuthModel;
import com.symphony.oss.models.auth.canon.INamedToken;
import com.symphony.oss.models.auth.canon.IToken;
import com.symphony.oss.models.auth.canon.PubkeyAuthenticatePostHttpRequest;
import com.symphony.oss.models.auth.canon.PubkeyAuthenticatePostHttpRequestBuilder;
import com.symphony.oss.models.auth.canon.Token;

/**
 * Handler for the bot RSA authentication mechanism.
 * 
 * This class attempts to extract the userId from the received token, if we can do this and if the returned userId is not an internal one
 * (i.e. we are not on one of the special pods with a different internal podId) we can avoid making calls to podInfo and accountInfo.
 * 
 * @author Bruce Skingle
 *
 */
public class AuthHandler implements IAuthHandler
{
  private static final Logger              log_        = LoggerFactory.getLogger(AuthHandler.class);
  private static final long                RETRY_LIMIT = 30000;
  
  private final CloseableHttpClient        podHttpClient_;
  private final CloseableHttpClient        kmHttpClient_;
  private final CookieStore                cookieStore_;
  private final PrivateKey                 rsaCredential_;
  private final IModelRegistry             modelRegistry_;
  private final IJwtAuthenticationProvider authProvider_;
  private AuthHttpModelClient              keyManagerClient_;
  private final AuthHttpModelClient        podClient_;

  private INamedToken                      keyManagerToken_;
  private INamedToken                      sessionToken_;
  private String                           podDomain_;
  private String                           keyManagerDomain_;
  private long                             sessionAuthTime_;
  private long                             reauthTime_;
  
  public AuthHandler(AllegroPodApi.AbstractBuilder<?, ?> builder, String serviceAccountName)
  {
    // builder.cookieStore_, builder.config_.getPodUrl(), builder.rsaCredential_
    
    podHttpClient_  = builder.getPodHttpClient();
    kmHttpClient_   = builder.getKeyManagerHttpClient();
    cookieStore_    = builder.cookieStore_;
    rsaCredential_  = builder.rsaCredential_;
    modelRegistry_  = new ModelRegistry().withFactories(AuthModel.FACTORIES);
    
    authProvider_   = new Rs512JwtGenerator(rsaCredential_)
        .withSubject(serviceAccountName)
        .withTTL(300000)  // 5 minutes, this is the max allowed by Symphony RSA authentication.
        ;
    
    podDomain_ = builder.config_.getPodUrl().getHost();
    
    podClient_ = new AuthHttpModelClient(
        modelRegistry_,
        builder.config_.getPodUrl(), "/login", null, null);
  }
  
  @Override
  public void close()
  {
    try
    {
      podHttpClient_.close();
      kmHttpClient_.close();
    }
    catch (IOException e)
    {
      log_.error("Unable to close HttpClient", e);
    }
  }

  @Override
  public synchronized ResponseHandlerAction reauthenticate(String usedSessionToken)
  {
    // If the token has changed then another thread reauthenticated and we can just retry.
    if(usedSessionToken != null && !usedSessionToken.equals(sessionToken_.getToken()))
      return ResponseHandlerAction.RETRY;
    
    // If we reauthenticated very recently then do nothing, the caller will just get the 401.
    
    if(System.currentTimeMillis() - reauthTime_ < RETRY_LIMIT)
      return ResponseHandlerAction.CONTINUE;
    
    reauthTime_ = System.currentTimeMillis();
    
    authenticate(true, true);
    return ResponseHandlerAction.RETRY;
  }

  @Override
  public long getSessionAuthTime()
  {
    return sessionAuthTime_;
  }

  @Override
  public synchronized void authenticate(boolean authSession, boolean authKeyManager)
  {
    String jwtToken = authProvider_.createJwt();
    IToken token;

    token = new Token.Builder()
      .withToken(jwtToken)
      .build();
    
    if(authSession)
    {
      sessionToken_    = authenticate(podHttpClient_, podClient_, token);
      addCookie("skey", sessionToken_, podDomain_);
      sessionAuthTime_ = System.currentTimeMillis();
    }
    
    if(authKeyManager)
    {
      keyManagerToken_ = authenticate(kmHttpClient_, keyManagerClient_, token);
      addCookie("kmsession", keyManagerToken_, keyManagerDomain_);
    }
  }

  @Override
  public String getKeyManagerToken()
  {
    authenticateIfNecessary();
    return keyManagerToken_.getToken();
  }

  @Override
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

  private void addCookie(String name, INamedToken sessionToken, String domain)
  {
    BasicClientCookie cookie = new BasicClientCookie(name, sessionToken.getToken());
    
    cookie.setDomain(domain);
    
    cookieStore_.addCookie(cookie);
  }

  private INamedToken authenticate(CloseableHttpClient httpClient, AuthHttpModelClient client, IToken token)
  {
    PubkeyAuthenticatePostHttpRequest request = new PubkeyAuthenticatePostHttpRequestBuilder(client)
        .withCanonPayload(token)
        .build();
      
      return request.execute(httpClient);
  }

  @Override
  public void setKeyManagerUrl(String keyManagerUrl)
  {
    keyManagerClient_ = new AuthHttpModelClient(
        modelRegistry_,
        keyManagerUrl, null, null, null);
    
    try
    {
      keyManagerDomain_ = new URL(keyManagerUrl).getHost();
    }
    catch (MalformedURLException e)
    {
      throw new IllegalStateException("Invalid KM URL received", e);
    }
  }
}
