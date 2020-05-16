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

package com.symphony.oss.allegro.ui;

class CacheEntry<E>
{
  private final E object_;
  private final RuntimeException exception_;
  
  CacheEntry(E object)
  {
    object_ = object;
    exception_ = null;
  }

  CacheEntry(RuntimeException exception)
  {
    object_ = null;
    exception_ = exception;
  }
  
  @Override
  public String toString()
  {
    if(object_ == null)
      return exception_.getClass().getSimpleName() + ": " + exception_.getLocalizedMessage();
    
    return object_.toString();
  }

  E get()
  {
    if(object_ == null)
      throw exception_;
    
    return object_;
  }

  E getObject()
  {
    return object_;
  }
}
