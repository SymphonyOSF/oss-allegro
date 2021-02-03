/*
 * Copyright 2021 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package com.symphony.oss.allegro.objectstore;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.symphony.oss.allegro.api.ILiveCurrentMessageConsumer;
import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.models.allegro.canon.facade.IAbstractReceivedChatMessage;

public class AllegroObjectStoreConsumerManager extends ObjectStoreConsumerManager
{
  private static final Logger log_ = LoggerFactory.getLogger(AllegroObjectStoreConsumerManager.class);
  
  private final ImmutableList<LiveCurrentConsumerHolder<?>>    liveCurrentConsumers_;
  
  AllegroObjectStoreConsumerManager(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    liveCurrentConsumers_ = ImmutableList.copyOf(builder.liveCurrentConsumers_);
  }
  
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AllegroObjectStoreConsumerManager> extends ObjectStoreConsumerManager.AbstractBuilder<T,B>
  {
    private final List<LiveCurrentConsumerHolder<?>>    liveCurrentConsumers_ = new LinkedList<>();
    
    AbstractBuilder(Class<T> type, ObjectStoreDecryptor decryptor, IModelRegistry modelRegistry)
    {
      super(type, decryptor, modelRegistry);
    }
    
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
