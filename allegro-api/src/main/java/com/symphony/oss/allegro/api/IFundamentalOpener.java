/*
 *
 *
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

package com.symphony.oss.allegro.api;

import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * An object capable of unwrapping and decrypting FundamentalObjects and SocialMessages.
 * 
 * @author Bruce Skingle
 *
 */
public interface IFundamentalOpener
{
//  /**
//   * Open (deserialize and decrypt if necessary) the given object.
//   * 
//   * @param item A FundamentalObject.
//   * 
//   * @return The typed contents of the given object.
//   */
//  IEntity open(IFundamentalObject item);
  
  /**
   * Create and IChatMessage from the given ILiveCurrentMessage, if the message
   * is an ISocialMessage then the message payload is decrypted.
   * 
   * @param message An ILiveCurrentMessage.
   * 
   * @return An IChatMessage representing the given message.
   */
  IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message);

  /**
   * Open (deserialize and decrypt) the given object.
   * 
   * @param storedApplicationObject An encrypted object.
   * 
   * @return The decrypted object.
   */
  IApplicationObjectPayload open(IStoredApplicationObject storedApplicationObject);
}
