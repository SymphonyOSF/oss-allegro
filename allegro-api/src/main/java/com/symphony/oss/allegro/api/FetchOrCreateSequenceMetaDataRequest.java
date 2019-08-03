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

package com.symphony.oss.allegro.api;

import com.symphony.oss.models.chat.canon.facade.ThreadId;

/**
 * Request object for FetchOrCreateSequenceMetaData.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchOrCreateSequenceMetaDataRequest extends SequenceMetaDataRequest<FetchOrCreateSequenceMetaDataRequest>
{
  private ThreadId      threadId_;
  
  /**
   * Constructor.
   */
  public FetchOrCreateSequenceMetaDataRequest()
  {
    super(FetchOrCreateSequenceMetaDataRequest.class);
  }
  
  /**
   * 
   * @return The thread ID for the sequence if created.
   */
  public ThreadId getThreadId()
  {
    return threadId_;
  }
  
  /**
   * Set the thread ID for the sequence if created.
   * 
   * @param threadId The thread ID for the sequence if created.
   * 
   * @return This (fluent method)
   */
  public FetchOrCreateSequenceMetaDataRequest withThreadId(ThreadId threadId)
  {
    threadId_ = threadId;
    
    return self();
  }
  
  @Override
  public void validate()
  {
    super.validate();
    require(threadId_, "Thread ID");
  }
}
