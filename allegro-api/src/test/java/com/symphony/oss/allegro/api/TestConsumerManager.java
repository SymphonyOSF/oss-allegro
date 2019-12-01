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

import static org.junit.Assert.assertEquals;

import java.time.temporal.ChronoUnit;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import org.symphonyoss.s2.canon.runtime.IEntity;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.NoOpTraceContextTransaction;
import org.symphonyoss.s2.fugue.pipeline.ISimpleConsumer;

import com.symphony.oss.allegro.api.request.ConsumerManager;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.IBaseSocialMaestroMessage;
import com.symphony.oss.models.chat.canon.IBaseSocialMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.chat.canon.IMaestroMessage;
import com.symphony.oss.models.chat.canon.ISignalNotification;
import com.symphony.oss.models.chat.canon.LiveCurrentMessageType;
import com.symphony.oss.models.chat.canon.MaestroEventType;
import com.symphony.oss.models.chat.canon.MaestroMessage;
import com.symphony.oss.models.chat.canon.facade.ISocialMessage;
import com.symphony.oss.models.chat.canon.facade.MessageId;
import com.symphony.oss.models.chat.canon.facade.SocialMessage;
import com.symphony.oss.models.core.canon.facade.PodId;
import com.symphony.oss.models.object.canon.facade.IApplicationObject;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

@SuppressWarnings("javadoc")
public class TestConsumerManager
{
//  private static final Class<?>[] APPLICATION_TYPES = {IPrincipal.class, Pod.class, IEnvironment.class, IFeed.class};
  private static final Class<?>[] LIVECURRENT_TYPES = {ISocialMessage.class, ISignalNotification.class, ILiveCurrentMessage.class};
  
  private static final PodId      POD_ID = PodId.newBuilder().build(1);
//  private static final IPod       POD = new Pod.Builder()
//      .withPodId(POD_ID)
//      .build();
  private static final ISocialMessage       SOCIAL_MESSAGE = new SocialMessage.Builder()
      .withVersion(LiveCurrentMessageType.SOCIALMESSAGE)
      .withMessageId(MessageId.newBuilder().build(Base64.decodeBase64("AAAA")))
      .withIngestionDate(System.currentTimeMillis())
      .withIsChime(false)
      .build();
  private static final IMaestroMessage       MAESTRO_MESSAGE = new MaestroMessage.Builder()
      .withVersion(LiveCurrentMessageType.MAESTRO)
      .withMessageId(MessageId.newBuilder().build(Base64.decodeBase64("BBBB")))
      .withIngestionDate(System.currentTimeMillis())
      .withEvent(MaestroEventType.CREATE_ROOM)
      .build();
//  private static final IFundamentalObject FUNDAMENTAL_OBJECT = new FundamentalObject.EntityObjectBuilder()
//      .withPayload(SOCIAL_MESSAGE)
//      .withPodId(POD_ID)
//      .withPurgeTime(10000, ChronoUnit.SECONDS)
//      .build();
  private static final IFundamentalOpener OPENER = new IFundamentalOpener()
  {
//    @Override
//    public IEntity open(IFundamentalObject item)
//    {
//      IFundamentalPayload payload = item.getPayload();
//      
////      if(payload instanceof IBlob)
////      {
////        IBlob blob = (IBlob)payload;
////        
////        IOpenSimpleSecurityContext securityContext = cryptoClient_.getSecurityContext(blob.getSecurityContextHash(), threadId);
////        
////        return modelRegistry_.open(item, securityContext);
////      }
////      else 
//        if(payload instanceof IClob)
//      {
//        return ((IClob)payload).getPayload();
//      }
//      else
//      {
//        return payload;
//      }
//    }

    @Override
    public IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message)
    {
      throw new RuntimeException("Not implemented");
    }

    @Override
    public IApplicationObject open(IStoredApplicationObject storedApplicationObject)
    {
      // TODO Auto-generated method stub
      return null;
    }
  };
  private static final ITraceContext trace = NoOpTraceContextTransaction.INSTANCE.open();
  
  private Class<?>  consumedType_;
  private Object    consumedObject_;
  
  private ConsumerManager createConsumerManager(Class<?> ...types)
  {
    ConsumerManager.Builder consumerManager =  new ConsumerManager.Builder();
    
    addTypes(consumerManager, types);
    
    return consumerManager.build();
  }
  
  private void addTypes(ConsumerManager.Builder consumerManager, Class<?>... types)
  {

    for(Class<?> type : types)
      consumerManager.withConsumer(type, (v, trace) -> 
      {
        consumedType_ = type;
        consumedObject_ = v;
      });
  }

//  @Test
//  public void testExact()
//  {
//    createConsumerManager(APPLICATION_TYPES).consume(POD, trace, OPENER);
//    
//    assertEquals(Pod.class, consumedType_);
//    assertEquals(POD, consumedObject_);
//  }
//
//  @Test
//  public void testClass()
//  {
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES);
//    
//    addTypes(consumerManager, LIVECURRENT_TYPES);
//    
//    consumerManager.consume(SOCIAL_MESSAGE, trace, OPENER);
//    
//    assertEquals(ISocialMessage.class, consumedType_);
//    assertEquals(SOCIAL_MESSAGE, consumedObject_);
//  }
//
//  @Test
//  public void testSubClass()
//  {
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES);
//    
//    addTypes(consumerManager, LIVECURRENT_TYPES);
//    
//    consumerManager.consume(MAESTRO_MESSAGE, trace, OPENER);
//    
//    assertEquals(ILiveCurrentMessage.class, consumedType_);
//    assertEquals(MAESTRO_MESSAGE, consumedObject_);
//  }
//  
//  @Test
//  public void testIntermediateClass()
//  {
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES);
//    
//    addTypes(consumerManager, ILiveCurrentMessage.class, IBaseSocialMessage.class, IBaseSocialMaestroMessage.class);
//    
//    consumerManager.consume(SOCIAL_MESSAGE, trace, OPENER);
//    
//    assertEquals(IBaseSocialMessage.class, consumedType_);
//    assertEquals(SOCIAL_MESSAGE, consumedObject_);
//  }
//  
//  @Test
//  public void testInvalid()
//  {
//    DC dc = new DC();
//    
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES)
//        .withDefaultConsumer(dc);
//    
//    consumerManager.consume(SOCIAL_MESSAGE, trace, OPENER);
//    
//    assertEquals(null, consumedType_);
//    assertEquals(null, consumedObject_);
//    assertEquals(SOCIAL_MESSAGE, dc.obj);
//  }
//  
//  class DC implements ISimpleConsumer<Object>
//  {
//    Object obj;
//    
//    @Override
//    public void consume(Object item, ITraceContext trace)
//    {
//      obj = item;
//    }
//  };
//  
//  @Test
//  public void testEntity()
//  {
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES);
//
//    addTypes(consumerManager, IEntity.class);
//    
//    consumerManager.consume(SOCIAL_MESSAGE, trace, OPENER);
//    
//    assertEquals(IEntity.class, consumedType_);
//    assertEquals(SOCIAL_MESSAGE, consumedObject_);
//  }
//  
//  @Test
//  public void testFundamentalApplication()
//  {
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES);
//
//    addTypes(consumerManager, ISocialMessage.class);
//    
//    consumerManager.consume(FUNDAMENTAL_OBJECT, trace, OPENER);
//    
//    assertEquals(ISocialMessage.class, consumedType_);
//    assertEquals(SOCIAL_MESSAGE, consumedObject_);
//  }
//  
//  @Test
//  public void testFundamentalClob()
//  {
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES);
//
//    addTypes(consumerManager, IClob.class);
//    
//    consumerManager.consume(FUNDAMENTAL_OBJECT, trace, OPENER);
//    
//    assertEquals(IClob.class, consumedType_);
//  }
//  
//  @Test
//  public void testFundamental()
//  {
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES);
//
//    addTypes(consumerManager, IFundamentalObject.class);
//    
//    consumerManager.consume(FUNDAMENTAL_OBJECT, trace, OPENER);
//    
//    assertEquals(IFundamentalObject.class, consumedType_);
//    assertEquals(FUNDAMENTAL_OBJECT, consumedObject_);
//  }
//  
//  @Test
//  public void testObject()
//  {
//    ConsumerManager consumerManager = createConsumerManager(APPLICATION_TYPES);
//
//    addTypes(consumerManager, Object.class);
//    
//    consumerManager.consume(FUNDAMENTAL_OBJECT, trace, OPENER);
//    
//    assertEquals(Object.class, consumedType_);
//    assertEquals(SOCIAL_MESSAGE, consumedObject_);
//  }
}
