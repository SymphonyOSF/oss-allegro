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

import com.symphony.oss.canon.runtime.ModelRegistry;

/**
 * A provider of a model registry.
 * 
 * @author Bruce Skingle
 *
 */
public interface IAllegroModelRegistryProvider
{
  /**
   * Return the ModelRegistry used by Allegro.
   * 
   * @return the ModelRegistry used by Allegro.
   */
  ModelRegistry getModelRegistry();
}
