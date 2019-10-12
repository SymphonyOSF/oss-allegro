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

/**
 * A request object for the FetchFeedMessages method.
 * 
 * For a more sophisticated implementation of this feature, which supports auto extension
 * and message deletion, see CreateFeedSubscriberRequest
 * 
 * @author Bruce Skingle
 *
 */
public class FetchFeedMessagesRequest extends ConsumerRequest<FetchFeedMessagesRequest>
{
  private Integer maxMessages_;
  private String  name_;
  
  /**
   * Constructor.
   */
  public FetchFeedMessagesRequest()
  {
    super(FetchFeedMessagesRequest.class);
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
  public FetchFeedMessagesRequest withMaxMessages(Integer maxMessages)
  {
    maxMessages_ = maxMessages;
    
    return self();
  }

  /**
   * 
   * @return The name of the feed to be read.
   */
  public String getName()
  {
    return name_;
  }

  /**
   * Set the name of the feed to be read.
   * 
   * @param name The name of the feed to be read.
   * 
   * @return This (fluent method)
   */
  public FetchFeedMessagesRequest withName(String name)
  {
    name_ = name;
    
    return self();
  }

  @Override
  public void validate()
  {
    super.validate();
    
    require(name_, "Name");
  }
}
