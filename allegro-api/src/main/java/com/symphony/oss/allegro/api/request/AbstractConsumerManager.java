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
import org.symphonyoss.s2.canon.runtime.exception.PermissionDeniedException;
import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IConsumer;
import org.symphonyoss.s2.fugue.pipeline.IErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.IRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.ISimpleErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.ISimpleRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeErrorConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.symphony.oss.allegro.api.IFundamentalOpener;
import com.symphony.oss.models.allegro.canon.facade.IChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * Multi Threaded, Asynchronous, Manager of Consumers.
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
public abstract class AbstractConsumerManager
{
  private static final Logger                                 log_ = LoggerFactory.getLogger(AbstractConsumerManager.class);

  private final ImmutableMap<Class<?>, IRetryableConsumer<?>> consumerMap_;
  private final ImmutableList<Class<?>>                       consumerTypeList_;
  private final boolean                                       hasApplicationTypes_;
  private final boolean                                       hasChatTypes_;
  private final IConsumer<Object>                             defaultConsumer_;
  private final IErrorConsumer<Object>                        unprocessableMessageConsumer_;
    
  AbstractConsumerManager(AbstractBuilder<?,?> builder)
  {
    consumerMap_          = ImmutableMap.copyOf(builder.consumerMap_);
    consumerTypeList_     = ImmutableList.copyOf(builder.consumerTypeList_);
    hasApplicationTypes_  = builder.hasApplicationTypes_;
    hasChatTypes_         = builder.hasChatTypes_;
    defaultConsumer_      = builder.defaultConsumer_;
    unprocessableMessageConsumer_   = builder.unprocessableMessageConsumer_;
  }
  
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractConsumerManager> extends BaseAbstractBuilder<T,B>
  {
    private Map<Class<?>, IRetryableConsumer<?>>             consumerMap_                  = new HashMap<>();
    private List<Class<?>>                                   consumerTypeList_             = new LinkedList<>();
    private boolean                                          hasApplicationTypes_;
    private boolean                                          hasChatTypes_;
    private IConsumer<Object>                                defaultConsumer_              = new IThreadSafeConsumer<Object>()
      {
        @Override
        public synchronized void consume(Object item, ITraceContext trace)
        {
          log_.error("No consumer found for object of type " + item.getClass() + "\n" + item);
        }
        
        @Override
        public void close(){}
      };

    private IErrorConsumer<Object> unprocessableMessageConsumer_ = new IThreadSafeErrorConsumer<Object>()
        {

          @Override
          public void consume(Object item, ITraceContext trace, String message,
              Throwable cause)
          {
            log_.error("Failed to process object of type " + item.getClass() + ": " + message + "\n" + item, cause);
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
    
    protected <C> T withConsumer(Class<C> type, IRetryableConsumer<C> consumer)
    {
      consumerMap_.put(type, consumer);
      consumerTypeList_.add(type);

      if(IChatMessage.class.isAssignableFrom(type))
        hasChatTypes_ = true;
      
      if(IApplicationObjectPayload.class.isAssignableFrom(type))
        hasApplicationTypes_ = true;
      
      return self();
    }
    
    
    @SuppressWarnings("unchecked")
    protected T withConsumerAdaptor(@SuppressWarnings("rawtypes") AbstractAdaptor adaptor)
    {
      adaptor.setDefaultConsumer(defaultConsumer_);
      
      return (T)withConsumer(adaptor.getPayloadType(), adaptor);
    }
    
    protected <C> T withConsumer(Class<C> type, ISimpleRetryableConsumer<C> consumer)
    {
      return withConsumer(type, new IRetryableConsumer<C>()
      {
        @Override
        public void consume(C item, ITraceContext trace) throws RetryableConsumerException, FatalConsumerException
        {
          consumer.consume(item, trace);
        }
        
        @Override
        public void close() {}
      });
    }
    
    protected T withDefaultConsumer(ISimpleRetryableConsumer<Object> defaultConsumer)
    {
      return withDefaultConsumer(new IRetryableConsumer<Object>()
      {
        @Override
        public void consume(Object item, ITraceContext trace) throws RetryableConsumerException, FatalConsumerException
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

    /**
     * Set the consumer to which unprocessable messages will be directed.
     * 
     * @param unprocessableMessageConsumer The consumer to which unprocessable messages will be directed.
     * 
     * @return This (fluent method)
     */
    protected T withUnprocessableMessageConsumer(IErrorConsumer<Object> unprocessableMessageConsumer)
    {
      unprocessableMessageConsumer_ = unprocessableMessageConsumer;
      
      return self();
    }

    /**
     * Set the consumer to which unprocessable messages will be directed.
     * 
     * @param unprocessableMessageConsumer The consumer to which unprocessable messages will be directed.
     * 
     * This convenience method accepts a non-closable consumer, which is a functional interface and is
     * convenient to use in cases where a close notification is not required.
     * 
     * @return This (fluent method)
     */
    protected T withUnprocessableMessageConsumer(ISimpleErrorConsumer<Object> unprocessableMessageConsumer)
    {
      unprocessableMessageConsumer_ = new IThreadSafeErrorConsumer<Object>()
          {

            @Override
            public void consume(Object item, ITraceContext trace, String message, Throwable cause)
            {
              unprocessableMessageConsumer.consume(item, trace, message, cause);
            }

            @Override
            public void close(){}
          };
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(unprocessableMessageConsumer_, "UnprocessableMessageConsumer must not be set to null (there is a default, you don't have to set one)");
    }
  }

  /**
   * 
   * @return All consumers.
   */
  public ImmutableCollection<IRetryableConsumer<?>> getConsumers()
  {
    return consumerMap_.values();
  }

  /**
   * Dispatch the given object to the most appropriate consumer.
   * 
   * @param object        An object to be consumed.
   * @param traceContext  A trace context.
   * @param opener        An opener to assist with decoding the object.
   * 
   * @throws FatalConsumerException       If thrown by the called consumer
   * @throws RetryableConsumerException   If thrown by the called consumer
   */
  public void consume(Object object, ITraceContext traceContext, IFundamentalOpener opener) throws RetryableConsumerException, FatalConsumerException
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
        try
        {
          IApplicationObjectPayload applicationObjectPayload = opener.open(storedApplicationObject);
          
          if(consumeChatTypes(applicationObjectPayload, traceContext, opener))
            return;
          
          if(hasApplicationTypes_)
          {
            if(consume(applicationObjectPayload, traceContext))
              return;
          }
        }
        catch(PermissionDeniedException e)
        {
          // can't decrypt
        }
      }
    }
    
    if(!consume(object, traceContext))
      defaultConsumer_.consume(object, traceContext);
  }
  
  private boolean consumeChatTypes(Object object, ITraceContext traceContext, IFundamentalOpener opener) throws RetryableConsumerException, FatalConsumerException
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
  private boolean consume(Object object, ITraceContext traceContext) throws RetryableConsumerException, FatalConsumerException
  {
    Class<? extends Object> type = object.getClass();
    IRetryableConsumer consumer = consumerMap_.get(type);
    
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
    
    ((IRetryableConsumer)consumerMap_.get(bestType)).consume(object, traceContext);
    return true;
  }

  /**
   * Close all consumers.
   * 
   * TODO: rename to close and implement AutoClosable.
   */
  public void closeConsumers()
  {
    for(IRetryableConsumer<?> consumer : consumerMap_.values())
      consumer.close();
    
    defaultConsumer_.close();
  }

  /**
   * 
   * @return The consumer to which unprocessable messages will be directed.
   */
  public IErrorConsumer<Object> getUnprocessableMessageConsumer()
  {
    return unprocessableMessageConsumer_;
  }
}
