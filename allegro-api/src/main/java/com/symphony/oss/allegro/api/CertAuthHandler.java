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

package com.symphony.oss.allegro.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.allegro.api.AllegroApi.AbstractBuilder;
import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.http.client.ResponseHandlerAction;
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
class CertAuthHandler implements IAuthHandler
{
  private static final Logger       log_             = LoggerFactory.getLogger(CertAuthHandler.class);
  private static final String       DEFAULT_PASSWORD = "changeit";
  private static final long         RETRY_LIMIT      = 30000;
  
  private final CloseableHttpClient        podHttpClient_;
  private final CloseableHttpClient        kmHttpClient_;
  private final CookieStore                cookieStore_;
  private final IModelRegistry             modelRegistry_;
  private final AuthHttpModelClient        keyManagerClient_;
  private final AuthHttpModelClient        podClient_;

  private INamedToken                      keyManagerToken_;
  private INamedToken                      sessionToken_;
  private String                           podDomain_;
  private String                           keyManagerDomain_;
  private long                             sessionAuthTime_;
  private long                             reauthTime_;
  
  CertAuthHandler(AbstractBuilder<?, ?> builder)
  {
    cookieStore_    = builder.cookieStore_;
    modelRegistry_  = new ModelRegistry().withFactories(AuthModel.FACTORIES);
    
    podDomain_ = builder.config_.getPodUrl().getHost();
    
    podClient_ = new AuthHttpModelClient(
        modelRegistry_,
        builder.config_.getSessionAuthUrl(), "/sessionauth/v1", null, null);
    
    keyManagerClient_ = new AuthHttpModelClient(
        modelRegistry_,
        builder.config_.getKeyAuthUrl(), "/keyauth/v1", null, null);
    
    try
    {
      KeyStore  keyStore = KeyStore.getInstance("PKCS12");
      String    keyStorePassword;
      
      if(builder.config_.getAuthCertFile() != null)
      {
        keyStorePassword = builder.config_.getAuthCertFilePassword();
        try (InputStream keyStoreStream = new FileInputStream(builder.config_.getAuthCertFile())) {
          
          keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
        }
      }
      else
      {
        keyStorePassword = DEFAULT_PASSWORD;
        X509Certificate[] certs = new X509Certificate[1];
        
        certs[0] = builder.cipherSuite_.certificateFromPem(builder.config_.getAuthCert());
        
        
        keyStore.load(null, keyStorePassword.toCharArray());
        
        keyStore.setKeyEntry("client", builder.cipherSuite_.privateKeyFromPem(builder.config_.getAuthCertPrivateKey()),
            keyStorePassword.toCharArray(), certs);
      }
      SSLContextBuilder sslContextBuilder = SSLContexts.custom()
          .loadKeyMaterial(keyStore, keyStorePassword.toCharArray());
  
      podHttpClient_  = builder.getCertSessionAuthHttpClient(sslContextBuilder);
      kmHttpClient_   = builder.getCertKeyAuthHttpClient(sslContextBuilder);
    }
    catch(IOException | GeneralSecurityException e)
    {
      throw new IllegalStateException("Unable to initialize client certificate", e);
    }
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
    
    if(reauthTime_ - System.currentTimeMillis() < RETRY_LIMIT)
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
    if(authSession)
    {
      sessionToken_    = authenticate(podHttpClient_, podClient_);
      addCookie("skey", sessionToken_, podDomain_);
      sessionAuthTime_ = System.currentTimeMillis();
    }
    
    if(authKeyManager)
    {
      keyManagerToken_ = authenticate(kmHttpClient_, keyManagerClient_);
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

  private INamedToken authenticate(CloseableHttpClient httpClient, AuthHttpModelClient client)
  {
    AuthenticatePostHttpRequest request = new AuthenticatePostHttpRequestBuilder(client)
        //.withCanonPayload(token)
        .build();
      
      return request.execute(httpClient);
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
