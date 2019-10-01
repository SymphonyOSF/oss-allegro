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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.symphonyoss.s2.common.hash.Hash;

import com.symphony.oss.models.system.canon.facade.FeedMessageAck;
import com.symphony.oss.models.system.canon.facade.FeedMessageNak;
import com.symphony.oss.models.system.canon.facade.IFeedMessageAck;
import com.symphony.oss.models.system.canon.facade.IFeedMessageNak;

public class FetchFeedMessagesRequest extends AllegroRequest<FetchFeedMessagesRequest>
{
  private int       maxMessages_ = 10;
  private Set<IFeedMessageAck> ackSet_      = new HashSet<>();
  private Set<IFeedMessageNak> nakSet_      = new HashSet<>();
  private String    name_;
  
  public FetchFeedMessagesRequest()
  {
    super(FetchFeedMessagesRequest.class);
  }

  public int getMaxMessages()
  {
    return maxMessages_;
  }

  public FetchFeedMessagesRequest withMaxMessages(int maxMessages)
  {
    maxMessages_ = maxMessages;
    
    return self();
  }

  public Set<IFeedMessageAck> getAckSet()
  {
    return ackSet_;
  }

  public FetchFeedMessagesRequest withAck(Collection<IFeedMessageAck> ackHashes)
  {
    ackSet_.addAll(ackHashes);
    
    return self();
  }

  public FetchFeedMessagesRequest withAck(IFeedMessageAck ackHash)
  {
    ackSet_.add(ackHash);
    
    return self();
  }

  public Set<IFeedMessageNak> getNakSet()
  {
    return nakSet_;
  }

  public FetchFeedMessagesRequest withNak(IFeedMessageNak nakHash)
  {
    nakSet_.add(nakHash);
    
    return self();
  }

  public FetchFeedMessagesRequest withNak(Collection<IFeedMessageNak> nakHashes)
  {
    nakSet_.addAll(nakHashes);
    
    return self();
  }

  public String getName()
  {
    return name_;
  }

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

  public void withAck(String receiptHandle)
  {
    ackSet_.add(new FeedMessageAck.Builder()
        .withReceiptHandle(receiptHandle)
        .build()
        );
  }
  


  public void withNak(String receiptHandle, int delaySeconds)
  {
    nakSet_.add(new FeedMessageNak.Builder()
        .withReceiptHandle(receiptHandle)
        .withNakDelay(delaySeconds)
        .build()
        );
  }
}
