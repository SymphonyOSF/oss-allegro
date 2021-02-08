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

package com.symphony.oss.allegro2.api;

import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.core.canon.facade.IApplicationRecord;
import com.symphony.oss.models.core.canon.facade.IEncryptedApplicationRecord;

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
public interface IAllegro2Decryptor
{
  /**
   * Create an IChatMessage from the given ILiveCurrentMessage, if the message
   * is an ISocialMessage then the message payload is decrypted.
   * 
   * @param message An ILiveCurrentMessage.
   * 
   * @return An IChatMessage representing the given message.
   */
  IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message);
  
  /**
   * Deserialize and decrypt the given object.
   * 
   * @param encryptedApplicationRecord An encrypted object.
   * 
   * @return The decrypted object.
   */
  IApplicationRecord decryptObject(IEncryptedApplicationRecord encryptedApplicationRecord);
}
