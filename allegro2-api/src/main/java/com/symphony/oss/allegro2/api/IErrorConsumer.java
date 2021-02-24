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

/**
 * Consumer for unprocessable records..
 * 
 * @author Bruce Skingle
 */
@FunctionalInterface
public interface IErrorConsumer
{
    /**
     * Consume the given decrypted record.
     *
     * @param record the source record. 
     * @param message the error description.
     * @param cause  An optional cause.
     */
    void accept(Object record, String message, Throwable cause);
}