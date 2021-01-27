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

import com.symphony.oss.models.core.canon.IApplicationPayload;

class StoredRecordConsumerHolder<H extends IApplicationPayload, P extends IApplicationPayload>
{
  Class<H>                         headerType_;
  Class<P>                         payloadType_;
  IApplicationRecordConsumer<H, P> consumer_;
  
  StoredRecordConsumerHolder(Class<H> headerType, Class<P> payloadType, IApplicationRecordConsumer<H, P> consumer)
  {
    headerType_ = headerType;
    payloadType_ = payloadType;
    consumer_ = consumer;
  }
}
