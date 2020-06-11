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

import com.symphony.oss.fugue.pipeline.FatalConsumerException;
import com.symphony.oss.fugue.pipeline.RetryableConsumerException;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedMaestroMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedSocialMessage;

/**
 * A consumer adaptor for ReceivedChatMessages.
 * 
 * Various typed onTYPENAME methods are defined and called when messages of each type are consumed.
 * The default implementations are NO-OPs so if some message types are to be ignored simply
 * do not override those handler methods.
 * 
 * @author Bruce Skingle
 *
 */
public class ReceivedChatMessageAdaptor extends AbstractAdaptor<IReceivedChatMessage>
{
  /**
   * Constructor.
   */
  public ReceivedChatMessageAdaptor()
  {
    super(IReceivedChatMessage.class);
  }

  @Override
  public final void consume(IReceivedChatMessage message, ITraceContext trace) throws RetryableConsumerException, FatalConsumerException
  {
    if(message instanceof IReceivedSocialMessage)
    {
      onSocialMessage((IReceivedSocialMessage) message, trace);
    }
    else if(message instanceof IReceivedMaestroMessage)
    {
      onMaestroMessage((IReceivedMaestroMessage) message, trace);
    }
    else
    {
      defaultConsumer_.consume(message, trace);
    }
  }

  @Override
  public void close()
  {
  }

  /**
   * Handle a ReceivedSocialMessage (an actual chat message)
   * 
   * @param message The message to be consumed.
   * @param trace A trace context.
   */
  public void onSocialMessage(IReceivedSocialMessage message, ITraceContext trace)
  {
  }

  /**
   * Handle a ReceivedMaestroMessage (a notification of some meta-event)
   * 
   * @param message The message to be consumed.
   * @param trace A trace context.
   */
  public void onMaestroMessage(IReceivedMaestroMessage message, ITraceContext trace)
  {
  }

}
