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

import java.io.Closeable;

import com.symphony.oss.canon.runtime.http.client.ResponseHandlerAction;

/**
 * @author Bruce Skingle
 *
 */
public interface IAuthHandler extends Closeable
{

  /**
   * Forces authentication
   * 
   * @param authSession true if one wants to refresh sessionToken 
   * @param authKeyManager true if one wants to refresh key manager token
   */
  void authenticate(boolean authSession, boolean authKeyManager);
  
  /**
   * @param usedSessionToken used to check which action perform
   * @return the action
   */
  ResponseHandlerAction reauthenticate(String usedSessionToken);

  /**
   * @return the key manager token
   */
  String getKeyManagerToken();

  /**
   * @return the session token
   */
  String getSessionToken();

  /**
   * @param keyManagerUrl the key manager url
   */
  void setKeyManagerUrl(String keyManagerUrl);

  /**
   * @return the time at which the authenticaton occurred
   */
  long getSessionAuthTime();

}
