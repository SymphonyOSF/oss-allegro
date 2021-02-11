/*
 *
 *
 * Copyright 2021 Symphony Communication Services, LLC.
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

package com.symphony.oss.allegro2.api;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.exception.PermissionDeniedException;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.allegro.canon.facade.IAbstractReceivedChatMessage;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.core.canon.facade.IApplicationRecord;
import com.symphony.oss.models.core.canon.facade.IEncryptedApplicationRecord;

/**
 * The manager of Allegro Consumers.
 * 
 * This class handles the routing of objects to the consumer with the closest type match to a received object.
 * 
 * @author Bruce Skingle
 *
 */
public class AllegroConsumerManager
{
  private static final Logger log_ = LoggerFactory.getLogger(AllegroConsumerManager.class);
  
  private final IAllegro2Decryptor                             allegroDecryptor_;
  private final IModelRegistry                                 modelRegistry_;
  private final ImmutableList<ApplicationConsumerHolder<?, ?>> applicationConsumers_;
  private final ImmutableList<LiveCurrentConsumerHolder<?>>    liveCurrentConsumers_;
  private final IErrorConsumer                                 errorConsumer_;

  protected AllegroConsumerManager(AbstractBuilder<?,?> builder)
  {
    allegroDecryptor_            = builder.decryptor_;
    modelRegistry_        = builder.modelRegistry_;
    applicationConsumers_ = ImmutableList.copyOf(builder.applicationConsumers_);
    liveCurrentConsumers_ = ImmutableList.copyOf(builder.liveCurrentConsumers_);
    errorConsumer_        = builder.errorConsumer_;
  }
  
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AllegroConsumerManager> extends BaseAbstractBuilder<T,B>
  {
    private final IAllegro2Decryptor                      decryptor_;
    private final IModelRegistry                        modelRegistry_;
    private final List<ApplicationConsumerHolder<?, ?>> applicationConsumers_ = new LinkedList<>();
    private final List<LiveCurrentConsumerHolder<?>>    liveCurrentConsumers_ = new LinkedList<>();
    private IErrorConsumer                              errorConsumer_        = new IErrorConsumer()
    {
      @Override
      public void accept(Object item, String message, Throwable cause)
      {
        log_.error(message + "\n" + item, cause);
      }
    };
    
    protected AbstractBuilder(Class<T> type, IAllegro2Decryptor decryptor, IModelRegistry modelRegistry)
    {
      super(type);
      
      Objects.nonNull(decryptor);
      Objects.nonNull(modelRegistry);
      
      decryptor_      = decryptor;
      modelRegistry_  = modelRegistry;
    }
    
    /**
     * Add the given IApplicationRecordConsumer to this manager.
     * 
     * @param <H>           The concrete type of the header to be consumed.
     * @param <P>           The concrete type of the payload to be consumed.
     * @param headerType    The concrete type of the header to be consumed.
     * @param payloadType   The concrete type of the payload to be consumed.
     * @param consumer      The consumer.
     * 
     * @return This (fluent method).
     */
    public <H extends IApplicationPayload, P extends IApplicationPayload> T withConsumer(Class<H> headerType, Class<P> payloadType, IApplicationRecordConsumer<H, P> consumer)
    {
      applicationConsumers_.add(new ApplicationConsumerHolder<H,P>(headerType, payloadType, consumer));
      
      return self();
    }
    
    T withConsumer(ApplicationConsumerHolder<?,?> holder)
    {
      applicationConsumers_.add(holder);
      
      return self();
    }
    
    /**
     * Add the given ILiveCurrentMessageConsumer to this manager.
     * 
     * @param <M>           The concrete type of the payload to be consumed.
     * @param payloadType   The concrete type of the payload to be consumed.
     * @param consumer      The consumer.
     * 
     * @return This (fluent method).
     */
    public <M extends IAbstractReceivedChatMessage> T withConsumer(Class<M> payloadType, ILiveCurrentMessageConsumer<M> consumer)
    {
      liveCurrentConsumers_.add(new LiveCurrentConsumerHolder<M>(payloadType, consumer));
      
      return self();
    }
    
    T withConsumer(LiveCurrentConsumerHolder<?> holder)
    {
      liveCurrentConsumers_.add(holder);
      
      return self();
    }
    
    /**
     * Set the error consumer to be used in the event that no valid consumer can be found for a message or the consumer
     * implementation throws a RuntimeException.
     * 
     * @param errorConsumer the error consumer to be used in the event that no valid consumer can be found for a message or the consumer
     * implementation throws a RuntimeException.
     * 
     * @return This (fluent method).
     */
    public T withErrorConsumer(IErrorConsumer errorConsumer)
    {
      errorConsumer_ = errorConsumer;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkNotNull(errorConsumer_,     "ErrorConsumer must not be set to null (there is a default, you don't have to set one)");
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, AllegroConsumerManager>
  {
    Builder(IAllegro2Decryptor allegroDecryptor, IModelRegistry modelRegistry)
    {
      super(Builder.class, allegroDecryptor, modelRegistry);
    }

    @Override
    protected AllegroConsumerManager construct()
    {
      return new AllegroConsumerManager(this);
    }
  }
  
  /**
   * Handle the given message or object.
   * 
   * @param json The JSON representation of a message or object.
   */
  public void accept(String json)
  {
    try
    {
      IEntity entity = modelRegistry_.parseOne(new StringReader(json));
      
      if(entity instanceof IEncryptedApplicationRecord)
      {
        accept((IEncryptedApplicationRecord)entity);
      }
      else if(entity instanceof ILiveCurrentMessage)
      {
        accept((ILiveCurrentMessage)entity);
      }
      else
      {
        errorConsumer_.accept(entity, "Unable to process record, not an IEncryptedApplicationRecord.", null);
      }
    }
    catch (RuntimeException e)
    {
      errorConsumer_.accept(json, "Unable to parse record", e);
    }
  }
  
  /**
   * Handle the given message.
   * 
   * @param lcmessage and ILiveCurrentMessage
   */
  public void accept(ILiveCurrentMessage lcmessage)
  {
    try
    {
      if(allegroDecryptor_ != null)
      {
        try
        {
          IReceivedChatMessage chatMessage = allegroDecryptor_.decryptChatMessage(lcmessage);

          if(chatMessage != null)
          {
              accept(lcmessage, chatMessage);
              return;
          }
        }
        catch(PermissionDeniedException e)
        {
          // can't decrypt
        }
      }
      
      accept(lcmessage, null);
    }
    catch (RuntimeException e)
    {
      errorConsumer_.accept(lcmessage, "Unable to process record", e);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void accept(ILiveCurrentMessage lcmessage, IReceivedChatMessage receivedMessage)
  {
    Class<? extends IReceivedChatMessage> payloadType  = receivedMessage == null ? null : receivedMessage.getClass();
    
        
    LiveCurrentConsumerHolder<?> bestConsumer = null;
    
    for(LiveCurrentConsumerHolder<?> t : liveCurrentConsumers_)
    { 
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
        // if the payload is a closer match we will take it
        if(payloadType != null && bestConsumer.payloadType_ != null && bestConsumer.payloadType_.isAssignableFrom(t.payloadType_))
        {
          bestConsumer = t;
        }

      }
    }
    
    if(bestConsumer == null)
    {
      errorConsumer_.accept(lcmessage, "No consumer for Application object " + lcmessage, null);
    }
    else
    {
      try
      {
        ((ILiveCurrentMessageConsumer)bestConsumer.consumer_).accept(lcmessage, receivedMessage);
      }
      catch(RuntimeException e)
      {
        errorConsumer_.accept(lcmessage, "Failed to process message", e);
      }
    }
  }
  
  /**
   * Handle the given object.
   * 
   * @param storedObject The encrypted object.
   */
  public void accept(IEncryptedApplicationRecord storedObject)
  {
    try
    {
      if(allegroDecryptor_ != null && storedObject.getEncryptedPayload() != null)
      {
        try
        {
          IApplicationRecord applicationRecord = allegroDecryptor_.decryptObject(storedObject);
          
          if(applicationRecord != null)
          {
              accept(storedObject, storedObject.getHeader(), applicationRecord.getPayload());
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
      errorConsumer_.accept(storedObject, "Unable to process record", e);
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void accept(IEncryptedApplicationRecord storedObject, IApplicationPayload header,
      IApplicationPayload payload)
  {

    Class<? extends IApplicationPayload> headerType   = header == null ? null : header.getClass();
    Class<? extends IApplicationPayload> payloadType  = payload == null ? null : payload.getClass();
    
        
    ApplicationConsumerHolder<?, ?> bestConsumer = null;
    
    for(ApplicationConsumerHolder<?, ?> t : applicationConsumers_)
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
        }
      }
    }
    
    if(bestConsumer == null)
    {
      errorConsumer_.accept(storedObject, "No consumer for header " + headerType +
          ", payload " + payloadType, null);
    }
    else
    {
      try
      {
        ((IApplicationRecordConsumer)bestConsumer.consumer_).accept(storedObject, header, payload);
      }
      catch(RuntimeException e)
      {
        errorConsumer_.accept(storedObject, "Failed to process message", e);
      }
    }
  }
}

class ApplicationConsumerHolder<H extends IApplicationPayload, P extends IApplicationPayload>
{
  Class<H>                         headerType_;
  Class<P>                         payloadType_;
  IApplicationRecordConsumer<H, P> consumer_;
  
  ApplicationConsumerHolder(Class<H> headerType, Class<P> payloadType, IApplicationRecordConsumer<H, P> consumer)
  {
    headerType_ = headerType;
    payloadType_ = payloadType;
    consumer_ = consumer;
  }
}

class LiveCurrentConsumerHolder<M extends IAbstractReceivedChatMessage>
{
  Class<M>                       payloadType_;
  ILiveCurrentMessageConsumer<M> consumer_;
  
  LiveCurrentConsumerHolder(Class<M> payloadType, ILiveCurrentMessageConsumer<M> consumer)
  {
    payloadType_ = payloadType;
    consumer_ = consumer;
  }
}
