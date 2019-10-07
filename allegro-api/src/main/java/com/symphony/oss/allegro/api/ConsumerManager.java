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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.canon.runtime.IEntity;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.IConsumer;
import org.symphonyoss.s2.fugue.pipeline.ISimpleConsumer;

import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.fundamental.canon.facade.IApplicationObject;
import com.symphony.oss.models.fundamental.canon.facade.IFundamentalObject;
import com.symphony.oss.models.fundamental.canon.facade.IFundamentalPayload;
import com.symphony.oss.models.fundamental.canon.facade.INotification;

class ConsumerManager
{
  private static final Logger log_ = LoggerFactory.getLogger(ConsumerManager.class);
  
  private Map<Class<?>, IConsumer<?>> consumerMap_      = new HashMap<>();
  private List<Class<?>>              consumerTypeList_ = new LinkedList<>();
  private boolean                     hasApplicationTypes_;
  private boolean                     hasPayloadTypes_;
  private boolean                     hasChatTypes_;
  private IConsumer<Object>           defaultConsumer_ = new IConsumer<Object>()
    {
      @Override
      public synchronized void consume(Object item, ITraceContext trace)
      {
        log_.error("No consumer found for message of type " + item.getClass() + "\n" + item);
      }
      
      @Override
      public void close(){}
    };
  
  public <T> ConsumerManager withConsumer(Class<T> type, IConsumer<T> consumer)
  {
    consumerMap_.put(type, consumer);
    consumerTypeList_.add(type);

    if(IChatMessage.class.isAssignableFrom(type))
      hasChatTypes_ = true;
    
    if(IApplicationObject.class.isAssignableFrom(type))
      hasApplicationTypes_ = true;
    
    if(IFundamentalPayload.class.isAssignableFrom(type))
      hasPayloadTypes_ = true;
    
    return this;
  }
  
  @SuppressWarnings("unchecked")
  public ConsumerManager withConsumer(@SuppressWarnings("rawtypes") AbstractAdaptor adaptor)
  {
    adaptor.setDefaultConsumer(defaultConsumer_);
    
    return withConsumer(adaptor.getPayloadType(), adaptor);
  }
  
  public <T> ConsumerManager withConsumer(Class<T> type, ISimpleConsumer<T> consumer)
  {
    return withConsumer(type, new IConsumer<T>()
    {
      @Override
      public void consume(T item, ITraceContext trace)
      {
        consumer.consume(item, trace);
      }
      
      @Override
      public void close() {}
    });
  }
  
  public ConsumerManager withDefaultConsumer(ISimpleConsumer<Object> defaultConsumer)
  {
    return withDefaultConsumer(new IConsumer<Object>()
    {
      @Override
      public void consume(Object item, ITraceContext trace)
      {
        defaultConsumer.consume(item, trace);
      }
      
      @Override
      public void close() {}
    });
  }
  
  public ConsumerManager withDefaultConsumer(IConsumer<Object> defaultConsumer)
  {
    defaultConsumer_ = defaultConsumer;
    
    return this;
  }

  public Collection<IConsumer<?>> getConsumers()
  {
    return consumerMap_.values();
  }

  public void consume(Object object, ITraceContext traceContext, IFundamentalOpener opener)
  {
    if(consumeChatTypes(object, traceContext, opener))
      return;
    
    IFundamentalObject fundamentalObject = null;
    
    if(object instanceof IFundamentalObject)
    {
      fundamentalObject = (IFundamentalObject) object;
    }
    else if(object instanceof INotification)
    {
      fundamentalObject = ((INotification)object).getPayload();
    }
    
    if(fundamentalObject != null)
    {
      if(hasChatTypes_ || hasApplicationTypes_)
      {
        IEntity entity = opener.open(fundamentalObject);
        
        if(consumeChatTypes(entity, traceContext, opener))
          return;
        
        if(hasApplicationTypes_)
        {
          if(consume(entity, traceContext))
            return;
        }
      }
      
      if(hasPayloadTypes_)
      {
        IFundamentalPayload payload = ((IFundamentalObject)object).getPayload();
        
        if(consume(payload, traceContext))
          return;
      }
    }
    
    if(!consume(object, traceContext))
      defaultConsumer_.consume(object, traceContext);
  }
  
  private boolean consumeChatTypes(Object object, ITraceContext traceContext, IFundamentalOpener opener)
  {
    if(hasChatTypes_ && object instanceof ILiveCurrentMessage)
    {
      IReceivedChatMessage chatMessage = opener.decryptChatMessage((ILiveCurrentMessage) object);
          
      try
      {
        consume(chatMessage, traceContext);
        return true;
      }
      catch(IllegalArgumentException e)
      {
        // Ignore
      }
    }
    
    return false;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private boolean consume(Object object, ITraceContext traceContext)
  {
    Class<? extends Object> type = object.getClass();
    IConsumer consumer = consumerMap_.get(type);
    
    if(consumer != null)
    {
      consumer.consume(object, traceContext);
    }
    
    Class<?> bestType = null;
    
    for(Class<?> t : consumerTypeList_)
    {
      if(!t.isAssignableFrom(type))
        continue;
      
      if(bestType == null || bestType.isAssignableFrom(t))
        bestType = t;
      
    }
    
    if(bestType == null)
      return false;
    
    ((IConsumer)consumerMap_.get(bestType)).consume(object, traceContext);
    return true;
  }

  public void closeConsumers()
  {
    for(IConsumer<?> consumer : consumerMap_.values())
      consumer.close();
    
    defaultConsumer_.close();
  }
}
