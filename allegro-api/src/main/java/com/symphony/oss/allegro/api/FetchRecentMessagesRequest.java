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

public class FetchRecentMessagesRequest
{
  private ThreadId  threadId_;
  private Integer   maxMessages_ = 51;
  
  public ThreadId getThreadId()
  {
    return threadId_;
  }
  
  public FetchRecentMessagesRequest withThreadId(ThreadId threadId)
  {
    threadId_ = threadId;
    
    return this;
  }
  
  public Integer getMaxMessages()
  {
    return maxMessages_;
  }
  
  public FetchRecentMessagesRequest withMaxMessages(Integer maxMessages)
  {
    maxMessages_ = maxMessages;
    
    return this;
  }
}
