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

package com.symphony.oss.allegro.api.request;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.IConsumer;
import org.symphonyoss.s2.fugue.pipeline.ISimpleConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.symphony.oss.allegro.api.IFundamentalOpener;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.object.canon.facade.IApplicationObject;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * Manager of Consumers.
 * 
 * When the consume method is called the consumer with the most specific type to the object
 * being consumed will be selected. Objects will be unwrapped if necessary to obtain a more
 * specific type to match with a consumer.
 * 
 * Sub types of the following types are considered more specific than the ones which follow:
 * <pre>
 * IChatMessage
 * IApplicationObject
 * </pre>
 * 
 * For example, if a StoredApplicationObject containing a SocialMessage was consumed, then consumers
 * would be selected in the following order of precedence:
 *
 * <pre>
 * IReceivedChatMessage
 * IChatMessage
 * ISocialMessage
 * ILiveCurrentMessage
 * IApplicationObject
 * IStoredApplicationObject
 * ISystemObject
 * Object
 * </pre>
 * 
 * In fact, when reading from the object store, the object being consumed will always be an instance of
 * IStoredApplicationObject and when reading from a feed the object will always be an instance of IEnvelope
 * so registering a consumer of those types represents a "catch all", but it is also possible to
 * register a handler of type Object if this is preferred.
 * 
 * @author Bruce Skingle
 */
public class ConsumerManager
{
  private static final Logger log_ = LoggerFactory.getLogger(ConsumerManager.class);
  
  private final ImmutableMap<Class<?>, IConsumer<?>> consumerMap_;
  private final ImmutableList<Class<?>>              consumerTypeList_;
  private final boolean                     hasApplicationTypes_;
  private final boolean                     hasChatTypes_;
  private final IConsumer<Object>           defaultConsumer_;
    
  protected ConsumerManager(AbstractBuilder<?,?> builder)
  {
    consumerMap_          = ImmutableMap.copyOf(builder.consumerMap_);
    consumerTypeList_     = ImmutableList.copyOf(builder.consumerTypeList_);
    hasApplicationTypes_  = builder.hasApplicationTypes_;
    hasChatTypes_         = builder.hasChatTypes_;
    defaultConsumer_      = builder.defaultConsumer_;
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, ConsumerManager>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected ConsumerManager construct()
    {
      return new ConsumerManager(this);
    }

    /**
     * Add the given consumer of the given payload type.
     * 
     * @param <C>       The type of the payload this consumer accepts.
     * @param type      The type of the payload this consumer accepts.
     * @param consumer  A consumer of the given type.
     * 
     * The <code>close()</code> method of all consumers will be called when the request completes.
     * 
     * @return This (fluent method).
     */
    @Override
    public <C> Builder withConsumer(Class<C> type, IConsumer<C> consumer)
    {
      return super.withConsumer(type, consumer);
    }

    /**
     * Add the given adaptor as a consumer.
     * 
     * In general, an adaptor will be a consumer for a super-class of some set of types for which it
     * adapts. It is possible to provide an Adaptor as well as specific Consumers, in this case the
     * Consumers will normally be called in preference to the adaptor since they register for more
     * specific types.
     * 
     * @param adaptor An adaptor.
     * 
     * @return This (fluent method).
     */
    public Builder withConsumer(AbstractAdaptor<?> adaptor)
    {
      return super.withConsumerAdaptor(adaptor);
    }

    /**
     * Add the given consumer of the given payload type.
     * 
     * @param <C>       The type of the payload this consumer accepts.
     * @param type      The type of the payload this consumer accepts.
     * @param consumer  A consumer of the given type.
     * 
     * This method accepts a simple consumer, which is a functional interface and is convenient
     * to use in cases where a close notification is not required.
     * 
     * @return This (fluent method).
     */
    @Override
    public <C> Builder withConsumer(Class<C> type, ISimpleConsumer<C> consumer)
    {
      return super.withConsumer(type, consumer);
    }

    /**
     * Set the consumer to be called with objects for which there is no match with any other consumer.
     * 
     * @param defaultConsumer  The consumer to be called with objects for which there is no match with any other consumer.
     * 
     * The default implementation (the default default) simply logs messages it consumes.
     * 
     * This method accepts a simple consumer, which is a functional interface and is convenient
     * to use in cases where a close notification is not required.
     * 
     * @return This (fluent method).
     */
    @Override
    public Builder withDefaultConsumer(ISimpleConsumer<Object> defaultConsumer)
    {
      return super.withDefaultConsumer(defaultConsumer);
    }

    /**
     * Add the consumer to be called with objects for which there is no match with any other consumer.
     * 
     * @param defaultConsumer  The consumer to be called with objects for which there is no match with any other consumer.
     * 
     * The default implementation (the default default) simply logs messages it consumes.
     * 
     * The <code>close()</code> method of all consumers will be called when the request completes.
     * 
     * @return This (fluent method).
     */
    @Override
    public Builder withDefaultConsumer(IConsumer<Object> defaultConsumer)
    {
      return super.withDefaultConsumer(defaultConsumer);
    }
  }
  
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends ConsumerManager> extends BaseAbstractBuilder<T,B>
  {
    private Map<Class<?>, IConsumer<?>> consumerMap_      = new HashMap<>();
    private List<Class<?>>              consumerTypeList_ = new LinkedList<>();
    private boolean                     hasApplicationTypes_;
    private boolean                     hasChatTypes_;
    private IConsumer<Object>           defaultConsumer_ = new IThreadSafeConsumer<Object>()
      {
        @Override
        public synchronized void consume(Object item, ITraceContext trace)
        {
          log_.error("No consumer found for message of type " + item.getClass() + "\n" + item);
        }
        
        @Override
        public void close(){}
      };
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    protected IConsumer<Object> getDefaultConsumer()
    {
      return defaultConsumer_;
    }
    
    protected <C> T withConsumer(Class<C> type, IConsumer<C> consumer)
    {
      consumerMap_.put(type, consumer);
      consumerTypeList_.add(type);

      if(IChatMessage.class.isAssignableFrom(type))
        hasChatTypes_ = true;
      
      if(IApplicationPayload.class.isAssignableFrom(type))
        hasApplicationTypes_ = true;
      
      return self();
    }
    
    
    @SuppressWarnings("unchecked")
    protected T withConsumerAdaptor(@SuppressWarnings("rawtypes") AbstractAdaptor adaptor)
    {
      adaptor.setDefaultConsumer(defaultConsumer_);
      
      return (T)withConsumer(adaptor.getPayloadType(), adaptor);
    }
    
    protected <C> T withConsumer(Class<C> type, ISimpleConsumer<C> consumer)
    {
      return withConsumer(type, new IConsumer<C>()
      {
        @Override
        public void consume(C item, ITraceContext trace)
        {
          consumer.consume(item, trace);
        }
        
        @Override
        public void close() {}
      });
    }
    
    protected T withDefaultConsumer(ISimpleConsumer<Object> defaultConsumer)
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
    
    protected T withDefaultConsumer(IConsumer<Object> defaultConsumer)
    {
      defaultConsumer_ = defaultConsumer;
      
      return self();
    }
  }

  /**
   * 
   * @return All consumers.
   */
  public ImmutableCollection<IConsumer<?>> getConsumers()
  {
    return consumerMap_.values();
  }

  /**
   * Dispatch the given object to the most appropriate consumer.
   * 
   * @param object        An object to be consumed.
   * @param traceContext  A trace context.
   * @param opener        An opener to assist with decoding the object.
   */
  public void consume(Object object, ITraceContext traceContext, IFundamentalOpener opener)
  {
    if(consumeChatTypes(object, traceContext, opener))
      return;
    
    IStoredApplicationObject storedApplicationObject = null;
    
    if(object instanceof IStoredApplicationObject)
    {
      storedApplicationObject = (IStoredApplicationObject) object;
    }
//    else if(object instanceof IEnvelope)
//    {
//      applicationPayload = ((IEnvelope)object).getPayload();
//    }
    
    if(storedApplicationObject != null)
    {
      if(hasChatTypes_ || hasApplicationTypes_)
      {
        IApplicationObject applicationObject = opener.open(storedApplicationObject);
        
        if(consumeChatTypes(applicationObject.getPayload(), traceContext, opener))
          return;
        
        if(hasApplicationTypes_)
        {
          if(consume(applicationObject.getPayload(), traceContext))
            return;
        }
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

  /**
   * Close all consumers.
   */
  public void closeConsumers()
  {
    for(IConsumer<?> consumer : consumerMap_.values())
      consumer.close();
    
    defaultConsumer_.close();
  }
}
