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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

/**
 * AuthHandler implementation using specified tokens.
 * 
 * @author Bruce Skingle
 *
 */
public class DummyAuthHandler implements IAuthHandler
{
  private final String      sessionToken_;
  private final String      keyManagerToken_;
  private final CookieStore cookieStore_;
  private final String      podDomain_;
  private String            keyManagerDomain_;
  
  public DummyAuthHandler(String sessionToken, String keyManagerToken, CookieStore cookieStore, URL podUrl)
  {
    sessionToken_ = sessionToken;
    keyManagerToken_ = keyManagerToken;
    cookieStore_    = cookieStore;
    
    podDomain_ = podUrl.getHost();
  }
  
  @Override
  public void close()
  {
  }

  @Override
  public void authenticate(boolean authSession, boolean authKeyManager)
  {
    if(authSession)
      addCookie("skey", sessionToken_, podDomain_);
    
    if(authKeyManager)
      addCookie("kmsession", keyManagerToken_, keyManagerDomain_);
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
