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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import com.symphony.oss.canon.runtime.http.client.ResponseHandlerAction;

/**
 * AuthHandler implementation using specified tokens.
 * 
 * @author Bruce Skingle
 *
 */
class DummyAuthHandler implements IAuthHandler
{
  private static final long      RETRY_LIMIT = 30000;

  private final Supplier<String> sessionTokenSupplier_;
  private final Supplier<String> keyManagerTokenSupplier_;
  private final CookieStore      cookieStore_;
  private final String           podDomain_;
  private String                 sessionToken_;
  private String                 keyManagerToken_;
  private String                 keyManagerDomain_;
  private long                   sessionAuthTime_;
  private long                   reauthTime_;
  
  DummyAuthHandler(Supplier<String> sessionTokenSupplier, Supplier<String> keyManagerTokenSupplier, CookieStore cookieStore, URL podUrl)
  {
    sessionTokenSupplier_ = sessionTokenSupplier;
    keyManagerTokenSupplier_ = keyManagerTokenSupplier;
    cookieStore_    = cookieStore;
    
    podDomain_ = podUrl.getHost();
  }
  
  @Override
  public void close()
  {
  }

  @Override
  public long getSessionAuthTime()
  {
    return sessionAuthTime_;
  }

  @Override
  public synchronized ResponseHandlerAction reauthenticate(String usedSessionToken)
  {
    // If the token has changed then another thread reauthenticated and we can just retry.
    if(usedSessionToken != null && !usedSessionToken.equals(sessionToken_))
      return ResponseHandlerAction.RETRY;
    
    // If we reauthenticated very recently then do nothing, the caller will just get the 401.
    
    if(reauthTime_ - System.currentTimeMillis() < RETRY_LIMIT)
      return ResponseHandlerAction.CONTINUE;
    
    reauthTime_ = System.currentTimeMillis();
    
    authenticate(true, true);
    return ResponseHandlerAction.RETRY;
  }

  @Override
  public synchronized void authenticate(boolean authSession, boolean authKeyManager)
  {
    if(authSession)
    {
      sessionToken_ = sessionTokenSupplier_.get();
      addCookie("skey", sessionToken_, podDomain_);
      sessionAuthTime_ = System.currentTimeMillis();
    }
    
    if(authKeyManager)
    {
      keyManagerToken_ = keyManagerTokenSupplier_.get();
      addCookie("kmsession", keyManagerToken_, keyManagerDomain_);
    }
  }

  private void addCookie(String name, String sessionToken, String domain)
  {
    BasicClientCookie cookie = new BasicClientCookie(name, sessionToken);
    
    cookie.setDomain(domain);
    
    cookieStore_.addCookie(cookie);
  }

  @Override
  public String getKeyManagerToken()
  {
    return keyManagerToken_;
  }

  @Override
  public String getSessionToken()
  {
    return sessionToken_;
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
