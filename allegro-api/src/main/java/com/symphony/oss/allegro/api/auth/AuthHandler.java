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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.http.client.IJwtAuthenticationProvider;
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
  private final CloseableHttpClient        httpClient_;
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
    
    podDomain_ = podUrl.getHost();
    
    podClient_ = new AuthHttpModelClient(
        modelRegistry_,
        podUrl, "/login", null);
  }
  
  @Override
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
      addCookie("skey", sessionToken_, podDomain_);
    }
    
    if(authKeyManager)
    {
      keyManagerToken_ = authenticate(keyManagerClient_, token);
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

  private INamedToken authenticate(AuthHttpModelClient client, IToken token)
  {
    PubkeyAuthenticatePostHttpRequest request = new PubkeyAuthenticatePostHttpRequestBuilder(client)
        .withCanonPayload(token)
        .build();
      
      return request.execute(httpClient_);
  }

  @Override
  public void setKeyManagerUrl(String keyManagerUrl)
  {
    keyManagerClient_ = new AuthHttpModelClient(
        modelRegistry_,
        keyManagerUrl, null, null);
    
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
