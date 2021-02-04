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

import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * Base class for AllegroApi implementations.
 * 
 * Methods which need to be able to decrypt objects take an instance of this type as a parameter.
 * 
 * Note that methods on this class may return null.
 * 
 * @author Bruce Skingle
 *
 */
abstract class AllegroDecryptor extends Allegro2Decryptor
{

//  /**
//   * Open (deserialize and decrypt) the given object.
//   * 
//   * @param storedApplicationObject An encrypted object.
//   * 
//   * @return The decrypted object.
//   */
//  @Override
//  public @Nullable IApplicationObjectPayload decryptObject(IEncryptedApplicationPayload storedApplicationObject)
//  {
//    return null;
//  }
  
  /**
   * Deserialize and decrypt the given object.
   * 
   * @param encryptedApplicationPayload An encrypted object.
   * 
   * @return The decrypted object.
   */
  public IApplicationObjectPayload decryptObject(IStoredApplicationObject encryptedApplicationPayload)
  {
    return null;
  }
}
