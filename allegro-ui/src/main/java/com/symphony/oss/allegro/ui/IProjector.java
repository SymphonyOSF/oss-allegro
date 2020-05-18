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

/**
 * A Projection is a set of attributes which a UI can operate upon. A Projector is a class which can extract a Projection from
 * some payload object.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The type of the payload class on which this projector operates.
 * @param <R> The type of the projection which it returns.
 */
@FunctionalInterface
public interface IProjector<T, R extends Projection<?>>
{
  /**
   * Create a projection of the given payload.
   * 
   * @param payload A payload.
   * 
   * @return a projection of the given payload.
   */
  R project(T payload);
}
