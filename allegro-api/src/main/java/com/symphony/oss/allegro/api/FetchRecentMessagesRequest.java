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
 * A request object for the FetchRecentMessages method.
 * 
 * @author Bruce Skingle
 *
 */
public class FetchRecentMessagesRequest extends AbstractFetchRecentMessagesRequest<FetchRecentMessagesRequest>
{
  /**
   * Constructor.
   */
  public FetchRecentMessagesRequest()
  {
    super(FetchRecentMessagesRequest.class);
  }
}

class AbstractFetchRecentMessagesRequest<T extends AbstractFetchRecentMessagesRequest<T>> extends ConsumerRequest<T>
{
  private ThreadId  threadId_;
  private Integer   maxMessages_ = 51;

  /**
   * Constructor.
   * 
   * @param type Concrete type for fluent methods.
   */
  public AbstractFetchRecentMessagesRequest(Class<T> type)
  {
    super(type);
  }

  /**
   * 
   * @return The id of the thread (conversation) from which messages are to be returned.
   */
  public ThreadId getThreadId()
  {
    return threadId_;
  }
  
  /**
   * Set the id of the thread (conversation) from which messages are to be returned.
   * 
   * @param threadId The id of the thread (conversation) from which messages are to be returned.
   * 
   * @return This (fluent method)
   */
  public T withThreadId(ThreadId threadId)
  {
    threadId_ = threadId;
    
    return self();
  }
  
  /**
   * 
   * @return The maximum number of messages to be returned.
   */
  public Integer getMaxMessages()
  {
    return maxMessages_;
  }
  
  /**
   * Set the maximum number of messages to be returned.
   * 
   * @param maxMessages The maximum number of messages to be returned.
   * 
   * @return This (fluent method)
   */
  public T withMaxMessages(Integer maxMessages)
  {
    maxMessages_ = maxMessages;
    
    return self();
  }
}
