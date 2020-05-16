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
 * A function which can extract UI applicable attributes from some object type.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The type of object which this function operates on.
 */
@FunctionalInterface
public interface IProjectionEnricher<T>
{
  /**
   * Project UI attributes for the given object into the given projection.
   * 
   * @param projection  Target for projected attributes.
   * @param payload     Object whose attributes are to be extracted.
   */
  void project(Projection.AttributeSet projection, T payload);
}
