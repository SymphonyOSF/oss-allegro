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

import java.util.List;

import javax.annotation.Nullable;

import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * A page of objects retrieved from the object store.
 * 
 * @author Bruce Skingle
 *
 */
public interface IObjectPage
{
  /**
   * It may be simpler to call <code>fetchNextPage()</code> rather than to use this method.
   * 
   * @return The token allowing a subsequent query to be made for the next page of objects.
   */
  String getAfter();

  /**
   * It may be simpler to call <code>fetchPrevPage()</code> rather than to use this method.
   * 
   * @return The token allowing a subsequent query to be made for the previous page of objects.
   */
  String getBefore();

  /**
   * Return the objects in this page of responses.
   * The returned value is actually an ImmutableList.
   * 
   * @return The objects in this page of responses.
   */
  List<IStoredApplicationObject> getData();

  /**
   * Dispatch all objects in this page to the consumers in the given consumer manager.
   * 
   * This method uses a NOOP trace context.
   * 
   * @param consumerManager A ConsumerManager (or AsyncConsumerManager) containing consumers to receive the page of objects.
   */
  void consume(AbstractConsumerManager consumerManager);

  /**
   * Dispatch all objects in this page to the consumers in the given consumer manager.
   * 
   * @param consumerManager A ConsumerManager (or AsyncConsumerManager) containing consumers to receive the page of objects.
   * @param trace           A trace context.
   */
  void consume(AbstractConsumerManager consumerManager, ITraceContext trace);

  /**
   * Fetch the next page of objects, if any.
   * 
   * @return The next page of objects, or <code>null</code> if there is none.
   */
  @Nullable IObjectPage fetchNextPage();

  /**
   * Fetch the previous page of objects, if any.
   * 
   * @return The previous page of objects, or <code>null</code> if there is none.
   */
  @Nullable IObjectPage fetchPrevPage();

}
