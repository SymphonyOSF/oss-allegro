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

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.symphony.oss.allegro.objectstore.IAllegroDecryptor;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.exception.PermissionDeniedException;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.allegro.canon.facade.IStoredApplicationRecord;
import com.symphony.oss.models.core.canon.IApplicationPayload;

/**
 * Base class of Manager of Consumers.
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
 * IAbstractStoredApplicationObject
 * IStoredApplicationObject
 * ISystemObject
 * Object
 * </pre>
 * 
 * In fact, when reading from the object store, the object being consumed will always be an instance of
 * IStoredApplicationObject or IDeletedApplicationObject and these are both sub-interfaces of IAbstractStoredApplicationObject,
 * so registering a consumer of those types represents a "catch all", but it is also possible to
 * register a handler of type Object if this is preferred.
 * 
 * When reading chat messages, the object being consumed will always be an instance of ILiveCurrentMessage.
 * 
 * @author Bruce Skingle
 */
public class StoredRecordConsumerManager
{
  private static final Logger                                 log_ = LoggerFactory.getLogger(StoredRecordConsumerManager.class);

  private final IAllegroDecryptor                                                               decryptor_;
  private final IModelRegistry                                                                  modelRegistry_;
  private final ImmutableList<ApplicationRecordConsumerHolder<?, ?>>                                 consumers_;
  private final IApplicationRecordConsumer<IApplicationPayload, IApplicationPayload> defaultConsumer_;
  private final IErrorConsumer                                                 unprocessableMessageConsumer_;
    
  StoredRecordConsumerManager(AbstractBuilder<?,?> builder)
  {
    decryptor_                    = builder.decryptor_;
    modelRegistry_                = builder.modelRegistry_;
    consumers_                    = ImmutableList.copyOf(builder.consumers_);
    defaultConsumer_              = builder.defaultConsumer_;
    unprocessableMessageConsumer_ = builder.unprocessableMessageConsumer_;
  }
  
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends StoredRecordConsumerManager> extends BaseAbstractBuilder<T,B>
  {
    private IAllegroDecryptor                                                               decryptor_;
    private IModelRegistry                                                                  modelRegistry_;
    private List<ApplicationRecordConsumerHolder<?, ?>>                                          consumers_            = new LinkedList<>();
    private IApplicationRecordConsumer<IApplicationPayload, IApplicationPayload> defaultConsumer_      = new IApplicationRecordConsumer<IApplicationPayload, IApplicationPayload> ()
    {
      @Override
      public void accept(IStoredApplicationRecord k, IApplicationPayload header, IApplicationPayload payload)
      {
        log_.error("No consumer found for " + (header == null ? "null header" : "header of type " + header.getClass()) + 
            " and " + ((payload == null ? "null payload" : "payload of type " + payload.getClass())));
      }
    };

    private IErrorConsumer unprocessableMessageConsumer_ = new IErrorConsumer()
    {
      @Override
      public void accept(Object item, String message, Throwable cause)
      {
        log_.error("Failed to process object of type " + item.getClass() + ": " + message + "\n" + item, cause);
      }
    };
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    public T withDecryptor(IAllegroDecryptor decryptor)
    {
      decryptor_ = decryptor;
      
      return self();
    }
    
    public T withModelRegistry(IModelRegistry modelRegistry)
    {
      modelRegistry_ = modelRegistry;
      
      return self();
    }
    
    public <H extends IApplicationPayload, P extends IApplicationPayload> T withConsumer(Class<H> headerType, Class<P> payloadType, IApplicationRecordConsumer<H, P> consumer)
    {
      return withConsumer(new ApplicationRecordConsumerHolder<H,P>(headerType, payloadType, consumer));
    }
    
    public T withConsumer(ApplicationRecordConsumerHolder<?,?> storedRecordConsumerHolder)
    {
      consumers_.add(storedRecordConsumerHolder);
      
      return self();
    }
    
    public T withDefaultConsumer(IApplicationRecordConsumer<IApplicationPayload, IApplicationPayload> defaultConsumer)
    {
      defaultConsumer_ = defaultConsumer;
      
      return self();
    }
    
    public T withUnprocessableMessageConsumer(IErrorConsumer errorConsumer)
    {
      unprocessableMessageConsumer_ = errorConsumer;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkNotNull(modelRegistry_,                "Model Registry");
      faultAccumulator.checkNotNull(decryptor_,                    "Decryptor");
      faultAccumulator.checkNotNull(unprocessableMessageConsumer_, "UnprocessableMessageConsumer must not be set to null (there is a default, you don't have to set one)");
    }
  }
  
  public static class Builder extends AbstractBuilder<Builder, StoredRecordConsumerManager>
  {
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected StoredRecordConsumerManager construct()
    {
      return new StoredRecordConsumerManager(this);
    }
  }
  
  public void accept(String json)
  {
    try
    {
      IEntity entity = modelRegistry_.parseOne(new StringReader(json));
      
      if(entity instanceof IStoredApplicationRecord)
      {
        accept((IStoredApplicationRecord)entity);
      }
      else
      {
        unprocessableMessageConsumer_.accept(entity, "Unable to process record, not an IStoredApplicationRecord.", null);
      }
    }
    catch (RuntimeException e)
    {
      unprocessableMessageConsumer_.accept(json, "Unable to parse record", e);
    }
  }

  public void accept(IStoredApplicationRecord storedObject)
  {
    try
    {
      if(decryptor_ != null && storedObject.getEncryptedPayload() != null)
      {
        try
        {
          IApplicationPayload applicationObjectPayload = decryptor_.decryptObject(storedObject);
          
          if(applicationObjectPayload != null)
          {
              accept(storedObject, storedObject.getHeader(), applicationObjectPayload);
              return;
          }
        }
        catch(PermissionDeniedException e)
        {
          // can't decrypt
        }
      }
      
      accept(storedObject, storedObject.getHeader(), null);
    }
    catch (RuntimeException e)
    {
      unprocessableMessageConsumer_.accept(storedObject, "Unable to process record", e);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void accept(IStoredApplicationRecord storedObject, IApplicationPayload header,
      IApplicationPayload payload)
  {

    Class<? extends IApplicationPayload> headerType   = header == null ? null : header.getClass();
    Class<? extends IApplicationPayload> payloadType  = payload == null ? null : payload.getClass();
    
        
    ApplicationRecordConsumerHolder<?, ?> bestConsumer = null;
    
    for(ApplicationRecordConsumerHolder<?, ?> t : consumers_)
    {
      if(headerType == null)
      {
        if(t.headerType_ != null)
        {
          continue;
        }
      }
      else
      {
        if(t.headerType_ == null || !t.headerType_.isAssignableFrom(headerType))
        {
          continue;
        }
      }
      
      if(payloadType == null)
      {
        if(t.payloadType_ != null)
        {
          continue;
        }
      }
      else
      {
        if(t.payloadType_ == null || !t.payloadType_.isAssignableFrom(payloadType))
        {
          continue;
        }
      }
      
      // This is a candidate
      
      if(bestConsumer == null)
      {
        bestConsumer = t;
      }
      else
      {
        // this is a candidate and we have a current candidate.
        
        if(bestConsumer.payloadType_.equals(t.payloadType_))
        {
          // It's the same payload type so if the header is a closer match we will take it
          if(headerType != null && bestConsumer.headerType_ != null && bestConsumer.headerType_.isAssignableFrom(t.headerType_))
          {
            bestConsumer = t;
          }
        }
        else
        {
          // if the payload is a closer match we will take it
          if(payloadType != null && bestConsumer.payloadType_ != null && bestConsumer.payloadType_.isAssignableFrom(t.payloadType_))
          {
            bestConsumer = t;
          }
//          else if(headerType != null && bestConsumer.headerType_ != null && bestConsumer.headerType_.isAssignableFrom(t.headerType_))
//          {
//            bestConsumer = t;
//          }
        }
      }
//        if(payloadType == null)
//      {
//        // If the headerType is a better match we will take it
//        if(headerType != null && bestConsumer.headerType_ != null && bestConsumer.headerType_.isAssignableFrom(headerType))
//        {
//          bestConsumer = t;
//        }
//      }
//      else 
//      {
//        if(headerType != null)
//        // If the headerType is a better match we will take it
//        if(headerType != null && bestConsumer.payloadType_ != null && bestConsumer.payloadType_.isAssignableFrom(headerType))
//      }
//        
//      if(!t.isAssignableFrom(type))
//        continue;
//      
//      if(bestType == null || bestType.isAssignableFrom(t))
//        bestType = t;
      
    }
    
    if(bestConsumer == null)
    {
      defaultConsumer_.accept(storedObject, header, payload);
    }
    else
    {
      ((IApplicationRecordConsumer)bestConsumer.consumer_).accept(storedObject, header, payload);
    }
  }

  /**
   * 
   * @return All consumers.
   */
  public ImmutableList<ApplicationRecordConsumerHolder<?, ?>> getConsumers()
  {
    return consumers_;
  }

//  /**
//   * Dispatch the given object to the most appropriate consumer.
//   * 
//   * @param object        An object to be consumed.
//   * @param opener        An opener to assist with decoding the object.
//   * 
//   * @throws FatalConsumerException       If thrown by the called consumer
//   * @throws RetryableConsumerException   If thrown by the called consumer
//   */
//  public void consume(Object object, IAllegroDecryptor opener) throws RetryableConsumerException, FatalConsumerException
//  {
//    if(consumeChatTypes(object, opener))
//      return;
//    
//    IEncryptedApplicationPayload storedApplicationObject = null;
//    
//    if(object instanceof IEncryptedApplicationPayload)
//    {
//      storedApplicationObject = (IEncryptedApplicationPayload) object;
//    }
//    
//    if(storedApplicationObject != null && storedApplicationObject.getEncryptedPayload() != null)
//    {
//      if(hasChatTypes_ || hasApplicationTypes_)
//      {
//        try
//        {
//          IApplicationObjectPayload applicationObjectPayload = opener.decryptObject(storedApplicationObject);
//          
//          if(applicationObjectPayload != null)
//          {
//            if(consumeChatTypes(applicationObjectPayload, traceContext, opener))
//              return;
//            
//            if(hasApplicationTypes_)
//            {
//              if(consume(applicationObjectPayload, traceContext))
//                return;
//            }
//          }
//        }
//        catch(PermissionDeniedException e)
//        {
//          // can't decrypt
//        }
//      }
//    }
//    
//    if(!consume(object, traceContext))
//      defaultConsumer_.consume(object, traceContext);
//  }
//  
//  private boolean consumeChatTypes(Object object, IAllegroDecryptor opener)
//  {
//    if(hasChatTypes_ && object instanceof ILiveCurrentMessage)
//    {
//      IReceivedChatMessage chatMessage = opener.decryptChatMessage((ILiveCurrentMessage) object);
//
//      if(chatMessage != null)
//      {
//        try
//        {
//          consume(chatMessage, null);
//          return true;
//        }
//        catch(IllegalArgumentException e)
//        {
//          // Ignore
//        }
//      }
//    }
//    
//    return false;
//  }
//
//  @SuppressWarnings({ "rawtypes", "unchecked" })
//  private boolean consume(Object object, ITraceContext traceContext) throws RetryableConsumerException, FatalConsumerException
//  {
//    Class<? extends Object> type = object.getClass();
//    IRetryableConsumer consumer = consumerMap_.get(type);
//    
//    if(consumer != null)
//    {
//      consumer.consume(object, traceContext);
//    }
//    
//    Class<?> bestType = null;
//    
//    for(Class<?> t : consumerTypeList_)
//    {
//      if(!t.isAssignableFrom(type))
//        continue;
//      
//      if(bestType == null || bestType.isAssignableFrom(t))
//        bestType = t;
//      
//    }
//    
//    if(bestType == null)
//      return false;
//    
//    ((IRetryableConsumer)consumerMap_.get(bestType)).consume(object, traceContext);
//    return true;
//  }
//
//  /**
//   * Close all consumers.
//   * 
//   * TODO: rename to close and implement AutoClosable.
//   */
//  public void closeConsumers()
//  {
//    for(IRetryableConsumer<?> consumer : consumerMap_.values())
//      consumer.close();
//    
//    defaultConsumer_.close();
//  }
//
//  /**
//   * 
//   * @return The consumer to which unprocessable messages will be directed.
//   */
//  public IErrorConsumer<Object> getUnprocessableMessageConsumer()
//  {
//    return unprocessableMessageConsumer_;
//  }
}
