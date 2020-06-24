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
  private static final Logger log_ = LoggerFactory.getLogger(CertAuthHandler.class);
  private static final String DEFAULT_PASSWORD = "changeit";
  
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
  
//  public CertAuthHandler(CookieStore cookieStore, URL podUrl, String keyStorePath, String keyStorePassword, URL sessionAuthUrl, URL keyManagerAuthUrl)
//  {
//    this(cookieStore, podUrl, readKeyStore(keyStorePath, keyStorePassword), keyStorePassword, sessionAuthUrl, keyManagerAuthUrl);
//  }
//  
//  public CertAuthHandler(CookieStore cookieStore, URL podUrl, X509Certificate authCert, PrivateKey authCertPrivateKey, URL sessionAuthUrl, URL keyManagerAuthUrl)
//  {
//    this(cookieStore, podUrl, createKeyStore(authCert, authCertPrivateKey), DEFAULT_PASSWORD, sessionAuthUrl, keyManagerAuthUrl);
//  }
//  
//  private CertAuthHandler(CookieStore cookieStore, URL podUrl, KeyStore keyStore, String keyStorePassword, URL sessionAuthUrl, URL keyManagerAuthUrl)
//  {
//    cookieStore_    = cookieStore;
//    modelRegistry_  = new ModelRegistry().withFactories(AuthModel.FACTORIES);
//    
//    podDomain_ = podUrl.getHost();
//    
//    podClient_ = new AuthHttpModelClient(
//        modelRegistry_,
//        sessionAuthUrl, "/sessionauth/v1", null);
//    
//    keyManagerClient_ = new AuthHttpModelClient(
//        modelRegistry_,
//        keyManagerAuthUrl, "/keyauth/v1", null);
//    try
//    {
//      sslContext_ = SSLContexts.custom()
//          .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
//          .build();
//  
//      httpClient_ = HttpClients.custom().setSSLContext(sslContext_).build();
//    }
//    catch(GeneralSecurityException e)
//    {
//      throw new IllegalStateException("Unable to initialize client certificate", e);
//    }
//    
//  }
  /*
   * if(builder.config_.getAuthCertFile() != null)
      return new CertAuthHandler(builder.cookieStore_, builder.config_.getPodUrl(),
          builder.config_.getAuthCertFile(), builder.config_.getAuthCertFilePassword(), builder.config_.getSessionAuthUrl(), builder.config_.getKeyAuthUrl());
    
    if(builder.config_.getAuthCert() != null)
    {
      return new CertAuthHandler(builder.cookieStore_, builder.config_.getPodUrl(),
          cipherSuite_.certificateFromPem(builder.config_.getAuthCert()),
          cipherSuite_.privateKeyFromPem(builder.config_.getAuthCertPrivateKey()),
          builder.config_.getSessionAuthUrl(), builder.config_.getKeyAuthUrl());
    }
   */

  CertAuthHandler(AbstractBuilder<?, ?> builder)
  {
    cookieStore_    = builder.cookieStore_;
    modelRegistry_  = new ModelRegistry().withFactories(AuthModel.FACTORIES);
    
    podDomain_ = builder.config_.getPodUrl().getHost();
    
    podClient_ = new AuthHttpModelClient(
        modelRegistry_,
        builder.config_.getSessionAuthUrl(), "/sessionauth/v1", null);
    
    keyManagerClient_ = new AuthHttpModelClient(
        modelRegistry_,
        builder.config_.getKeyAuthUrl(), "/keyauth/v1", null);
    
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

//  static KeyStore readKeyStore(String keyStorePath, String keyStorePassword)
//  {
//    try (InputStream keyStoreStream = new FileInputStream(keyStorePath)) {
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
//        keyStore.load(keyStoreStream, keyStorePassword.toCharArray());
//        return keyStore;
//    }
//    catch(GeneralSecurityException | IOException e)
//    {
//      throw new IllegalStateException("Unable to read client certificate", e);
//    }
//  }
  
//  static KeyStore createKeyStore(X509Certificate authCert, PrivateKey authCertPrivateKey)
//  {
//    X509Certificate[] certs = new X509Certificate[1];
//    
//    certs[0] = authCert;
//    
//    try
//    {
//      KeyStore keyStore = KeyStore.getInstance("PKCS12");
//      keyStore.load(null, DEFAULT_PASSWORD.toCharArray());
//      
//      keyStore.setKeyEntry("client", authCertPrivateKey, DEFAULT_PASSWORD.toCharArray(), certs);
//      
//      return keyStore;
//    }
//    catch(GeneralSecurityException | IOException e)
//    {
//      throw new IllegalStateException("Unable to read client certificate", e);
//    }
//  }
  
  @Override
  public void authenticate(boolean authSession, boolean authKeyManager)
  {
    if(authSession)
    {
      sessionToken_    = authenticate(podHttpClient_, podClient_);
      addCookie("skey", sessionToken_, podDomain_);
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
