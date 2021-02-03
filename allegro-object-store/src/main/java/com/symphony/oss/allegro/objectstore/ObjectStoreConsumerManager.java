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

package com.symphony.oss.allegro.objectstore;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.symphony.oss.allegro.api.IErrorConsumer;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.exception.PermissionDeniedException;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

public class ObjectStoreConsumerManager
{
  private static final Logger log_ = LoggerFactory.getLogger(ObjectStoreConsumerManager.class);
  
  private final ObjectStoreDecryptor                               decryptor_;
  private final IModelRegistry                                 modelRegistry_;
  private final ImmutableList<ApplicationConsumerHolder<?, ?>> applicationConsumers_;
  private final IErrorConsumer                                 errorConsumer_;

  ObjectStoreConsumerManager(AbstractBuilder<?,?> builder)
  {
    decryptor_            = builder.decryptor_;
    modelRegistry_        = builder.modelRegistry_;
    applicationConsumers_ = ImmutableList.copyOf(builder.applicationConsumers_);
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
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends ObjectStoreConsumerManager> extends BaseAbstractBuilder<T,B>
  {
    private final ObjectStoreDecryptor                      decryptor_;
    private final IModelRegistry                        modelRegistry_;
    private final List<ApplicationConsumerHolder<?, ?>> applicationConsumers_ = new LinkedList<>();
    private IErrorConsumer                              errorConsumer_        = new IErrorConsumer()
    {
      @Override
      public void accept(Object item, String message, Throwable cause)
      {
        log_.error("Failed to process object of type " + item.getClass() + ": " + message + "\n" + item, cause);
      }
    };
    
    AbstractBuilder(Class<T> type, ObjectStoreDecryptor decryptor, IModelRegistry modelRegistry)
    {
      super(type);
      
      Objects.nonNull(decryptor);
      Objects.nonNull(modelRegistry);
      
      decryptor_      = decryptor;
      modelRegistry_  = modelRegistry;
    }
    
    public <H extends IApplicationObjectHeader, P extends IApplicationObjectPayload> T withConsumer(Class<H> headerType, Class<P> payloadType, IApplicationObjectConsumer<H, P> consumer)
    {
      applicationConsumers_.add(new ApplicationConsumerHolder<H,P>(headerType, payloadType, consumer));
      
      return self();
    }
    
    T withConsumer(ApplicationConsumerHolder<?,?> holder)
    {
      applicationConsumers_.add(holder);
      
      return self();
    }
    
    public T withErrorConsumer(IErrorConsumer errorConsumer)
    {
      errorConsumer_ = errorConsumer;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

//      faultAccumulator.checkNotNull(modelRegistry_,     "Model Registry");
//      faultAccumulator.checkNotNull(decryptor_,  "Decryptor");
      faultAccumulator.checkNotNull(errorConsumer_,     "ErrorConsumer must not be set to null (there is a default, you don't have to set one)");
    }
  }
  
  public static class Builder extends AbstractBuilder<Builder, ObjectStoreConsumerManager>
  {
    public Builder(ObjectStoreDecryptor allegroDecryptor, IModelRegistry modelRegistry)
    {
      super(Builder.class, allegroDecryptor, modelRegistry);
    }

    @Override
    protected ObjectStoreConsumerManager construct()
    {
      return new ObjectStoreConsumerManager(this);
    }
  }
  
  public void accept(String json)
  {
    try
    {
      IEntity entity = modelRegistry_.parseOne(new StringReader(json));
      
      if(entity instanceof IStoredApplicationObject)
      {
        accept((IStoredApplicationObject)entity);
      }
      else
      {
        errorConsumer_.accept(entity, "Unable to process record, not an IEncryptedApplicationObject.", null);
      }
    }
    catch (RuntimeException e)
    {
      errorConsumer_.accept(json, "Unable to parse record", e);
    }
  }

  public void accept(IStoredApplicationObject storedObject)
  {
    try
    {
      if(decryptor_ != null && storedObject.getEncryptedPayload() != null)
      {
        try
        {
          IApplicationObjectPayload applicationPayload = decryptor_.decryptObject(storedObject);
          
          if(applicationPayload != null)
          {
              accept(storedObject, storedObject.getHeader(), applicationPayload);
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
  private void accept(IStoredApplicationObject storedObject, IApplicationObjectHeader header,
      IApplicationObjectPayload payload)
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
      errorConsumer_.accept(storedObject, "No consumer for Application object " + storedObject, null);
    }
    else
    {
      try
      {
        ((IApplicationObjectConsumer)bestConsumer.consumer_).accept(storedObject, header, payload);
      }
      catch(RuntimeException e)
      {
        errorConsumer_.accept(storedObject, "Failed to process message", e);
      }
    }
  }
}

class ApplicationConsumerHolder<H extends IApplicationObjectHeader, P extends IApplicationObjectPayload>
{
  Class<H>                         headerType_;
  Class<P>                         payloadType_;
  IApplicationObjectConsumer<H, P> consumer_;
  
  ApplicationConsumerHolder(Class<H> headerType, Class<P> payloadType, IApplicationObjectConsumer<H, P> consumer)
  {
    headerType_ = headerType;
    payloadType_ = payloadType;
    consumer_ = consumer;
  }
}
//
//class LiveCurrentConsumerHolder<M extends IAbstractReceivedChatMessage>
//{
//  Class<M>                       payloadType_;
//  ILiveCurrentMessageConsumer<M> consumer_;
//  
//  LiveCurrentConsumerHolder(Class<M> payloadType, ILiveCurrentMessageConsumer<M> consumer)
//  {
//    payloadType_ = payloadType;
//    consumer_ = consumer;
//  }
//}
