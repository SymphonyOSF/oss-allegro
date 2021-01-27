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

import javax.annotation.Nullable;

import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * A page of objects retrieved from a partition in the object store.
 * 
 * @author Bruce Skingle
 *
 */
public interface IObjectPage extends IAbstractObjectPage<IStoredApplicationObject>
{
  /**
   * Fetch the next page of objects, if any.
   * 
   * @return The next page of objects, or <code>null</code> if there is none.
   */
  @Override
  @Nullable IObjectPage fetchNextPage();

  /**
   * Fetch the previous page of objects, if any.
   * 
   * @return The previous page of objects, or <code>null</code> if there is none.
   */
  @Override
  @Nullable IObjectPage fetchPrevPage();
}
