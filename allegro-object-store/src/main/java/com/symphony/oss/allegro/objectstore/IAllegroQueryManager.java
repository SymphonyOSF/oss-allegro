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

import com.symphony.oss.fugue.IFugueLifecycleComponent;

/**
 * A manager of some query.
 * 
 * In the case of asynchronous queries, this component manages the thread pools to do the work.
 * 
 * @author Bruce Skingle
 *
 */
public interface IAllegroQueryManager extends IFugueLifecycleComponent
{
  /**
   * Return true if the QueryManager is idle, meaning that there is no data available to process.
   * 
   * In the case of a database query, being idle indicates that there is no more data to consume, therefore
   * in these cases this method never returns false once it has returned true.
   * In the case of a feed query, being idle indicates that there is no more data available for the moment,
   * but more data could become available at any time. 
   * 
   * @return true iff this QueryManager is idle.
   */
  boolean isIdle();

  /**
   * Block until this manager becomes idle.
   * 
   * See the <code>isIdle()</code> for more information about the meaning of "idle".
   * 
   * @throws InterruptedException If the wait is interrupted.
   */
  void waitUntilIdle() throws InterruptedException;
}
