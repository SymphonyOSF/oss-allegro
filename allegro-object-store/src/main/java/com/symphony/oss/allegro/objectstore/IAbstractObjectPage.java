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

package com.symphony.oss.allegro.objectstore;

import java.util.List;

import javax.annotation.Nullable;

import com.symphony.oss.allegro.api.AbstractConsumerManager;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;

/**
 * A page of objects retrieved from the object store.
 * 
 * When the list comes from a partition T will be IStoredApplicationObject but when the page comes from
 * a version query T will be IAbstractStoredApplicationObject because there may be delete markers
 * in the data.
 * 
 * @param <T> The type of objects in the page. 
 * 
 * @author Bruce Skingle
 *
 */
public interface IAbstractObjectPage<T extends IAbstractStoredApplicationObject>
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
  List<T> getData();

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
  @Nullable IAbstractObjectPage<T> fetchNextPage();

  /**
   * Fetch the previous page of objects, if any.
   * 
   * @return The previous page of objects, or <code>null</code> if there is none.
   */
  @Nullable IAbstractObjectPage<T> fetchPrevPage();

}
