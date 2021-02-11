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

import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.core.canon.facade.IEncryptedApplicationRecord;

/**
 * Consumer for decrypted StoredApplicationRecords.
 * 
 * @author Bruce Skingle
 *
 * @param <H> The type of the unencrypted header.
 * @param <P> The type of the decrypted payload.
 */
@FunctionalInterface
public interface IApplicationRecordConsumer<H extends IApplicationPayload, P extends IApplicationPayload>
{
    /**
     * Consume the given decrypted record.
     *
     * @param record the record as stored (with encrypted payload). 
     * @param header the unencrypted header.
     * @param payload the decrypted payload.
     */
    void accept(IEncryptedApplicationRecord record, H header, P payload);
}