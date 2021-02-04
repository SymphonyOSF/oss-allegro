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

package com.symphony.oss.allegro.api;

import com.symphony.oss.models.allegro.canon.facade.IAbstractReceivedChatMessage;

/**
 * Asynchronous consumer for decrypted LiveCurrentMessages.
 * 
 * @author Bruce Skingle
 *
 * @param <M> The type of the decrypted message.
 */
@FunctionalInterface
public interface IAsyncLiveCurrentMessageConsumer<M extends IAbstractReceivedChatMessage>
extends IAbstractLiveCurrentMessageConsumer<M>, IAsyncConsumer
{

}
