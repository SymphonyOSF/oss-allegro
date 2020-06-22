/*
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

package com.symphony.oss.allegro.api.auth;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContexts;

import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.models.auth.canon.AuthHttpModelClient;
import com.symphony.oss.models.auth.canon.AuthModel;
import com.symphony.oss.models.auth.canon.AuthenticatePostHttpRequest;
import com.symphony.oss.models.auth.canon.AuthenticatePostHttpRequestBuilder;
import com.symphony.oss.models.auth.canon.INamedToken;

/**
 * Handler for the bot Certificate authentication mechanism.
 * 
 * This class attempts to extract the userId from the received token, if we can do this and if the returned userId is not an internal one
 * (i.e. we are not on one of the special pods with a different internal podId) we can avoid making calls to podInfo and accountInfo.
 * 
 * @author Bruce Skingle
 *
 */
public class CertAuthHandler implements IAuthHandler
{
  private final CloseableHttpClient        httpClient_;
  private final CookieStore                cookieStore_;
  private final IModelRegistry             modelRegistry_;
  private final AuthHttpModelClient        keyManagerClient_;
  private final AuthHttpModelClient        podClient_;

  private INamedToken                      keyManagerToken_;
  private INamedToken                      sessionToken_;
  private String                           podDomain_;
  private String                           keyManagerDomain_;
  private SSLContext sslContext_;
  
  public CertAuthHandler(CookieStore cookieStore, URL podUrl, String keyStorePath, String keyStorePassword, String sessionAuthUrl, String keyManagerAuthUrl)
  {
    cookieStore_    = cookieStore;
    modelRegistry_  = new ModelRegistry().withFactories(AuthModel.FACTORIES);
    
    podDomain_ = podUrl.getHost();
    
    podClient_ = new AuthHttpModelClient(
        modelRegistry_,
        sessionAuthUrl, "/sessionauth/v1", null);
    
    keyManagerClient_ = new AuthHttpModelClient(
        modelRegistry_,
        keyManagerAuthUrl, "/keyauth/v1", null);
    try
    {
      sslContext_ = SSLContexts.custom()
          .loadKeyMaterial(readStore(keyStorePath, keyStorePassword), keyStorePassword.toCharArray())
          .build();
  
      httpClient_ = HttpClients.custom().setSSLContext(sslContext_).build();
    }
    catch(GeneralSecurityException | IOException e)
    {
      throw new IllegalStateException("Unable to read client certificate", e);
    }
  }
  
  KeyStore readStore(String keyStorePath, String keyStorePassword) throws GeneralSecurityException, IOException
  {
    try (InputStream keyStoreStream = new FileInputStream(keyStorePath)) {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
        return keyStore;
    }
}
  
  @Override
  public void authenticate(boolean authSession, boolean authKeyManager)
  {
    if(authSession)
    {
      sessionToken_    = authenticate(podClient_);
      addCookie("skey", sessionToken_, podDomain_);
    }
    
    if(authKeyManager)
    {
      keyManagerToken_ = authenticate(keyManagerClient_);
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

  private INamedToken authenticate(AuthHttpModelClient client)
  {
    AuthenticatePostHttpRequest request = new AuthenticatePostHttpRequestBuilder(client)
        //.withCanonPayload(token)
        .build();
      
      return request.execute(httpClient_);
  }

  @Override
  public void setKeyManagerUrl(String keyManagerUrl)
  {
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
