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

package com.symphony.oss.allegro.objectstore;

import com.symphony.oss.models.allegro.canon.facade.IObjectStoreConfiguration;

/**
 * The public interface of the Multi-Tenant Allegro API.
 * <p>
 * The multi-tenant version of AllegroApi does not support any encryption or decryption capabilities and
 * does not require authentication to a pod. This is intended for use by multi-tenant services. For the
 * full API see {@link IAllegroObjectStoreApi}.
 * <p>
 * Generally methods called {@code getXXX()} return something from the local client whereas methods
 * called {@code fetchXXX()} involve a network request to some server.
 * <p>
 * @author Bruce Skingle
 */
public interface IObjectStoreApi extends IBaseObjectStoreApi
{

  /**
   * Return the current configuration.
   * 
   * @return The current configuration.
   */
  IObjectStoreConfiguration getConfiguration();

}
