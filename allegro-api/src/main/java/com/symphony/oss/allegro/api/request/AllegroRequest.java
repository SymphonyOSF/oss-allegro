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

package com.symphony.oss.allegro.api.request;

import org.symphonyoss.s2.common.fluent.Fluent;

/**
 * Base class for Allegro request objects.
 * 
 * @param <T> The concrete type returned by fluent methods.
 * 
 * @author Bruce Skingle
 *
 */
@Deprecated
public class AllegroRequest<T extends AllegroRequest<T>> extends Fluent<T>
{
  protected AllegroRequest(Class<T> type)
  {
    super(type);
  }

  protected void require(Object value, String name)
  {
    if(value == null)
      throw new IllegalArgumentException(name + " is required.");
  }
  
  /**
   * Validate the request.
   * 
   * @throws IllegalArgumentException if any values are invalid.
   */
  public void validate()
  {}
}
