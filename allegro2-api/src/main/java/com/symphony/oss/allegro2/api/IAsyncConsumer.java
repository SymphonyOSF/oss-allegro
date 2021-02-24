/*
 *
 *
 * Copyright 2021 Symphony Communication Services, LLC.
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

/**
 * A consumer for use with an asynchronous consumer manager.
 * 
 * The accept method(s) on implementations must be thread safe.
 * 
 * @author Bruce Skingle
 *
 */
public interface IAsyncConsumer extends IConsumer, AutoCloseable
{
  /**
   * Will be called after the final call to any accept method.
   * 
   * The default implementation allows sub-interfaces to be @Functional.
   */
  @Override
  default void close()
  {
  }

 

}
