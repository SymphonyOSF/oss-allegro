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

package com.symphony.oss.allegro.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.models.allegro.canon.facade.IReceivedChatMessage;
import com.symphony.oss.models.chat.canon.ILiveCurrentMessage;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.ObjectModel;
import com.symphony.oss.models.object.canon.facade.ApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.ApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationRecord;
import com.symphony.oss.models.object.canon.facade.StoredApplicationRecord;

public class TestStoredRecordConsumerManager
{
  class Header1 extends ApplicationObjectHeader  {

    public Header1()
    {
      super(new ApplicationObjectHeader.Builder());
    }
  }
  class Header2 extends Header1  {}
  class Header3 extends Header2  {}
  
  class Payload1 extends ApplicationObjectPayload  {

    public Payload1()
    {
      super(new ApplicationObjectPayload.Builder());
    }}
  class Payload2 extends Payload1  {}
  class Payload3 extends Payload2  {}
  

  private Class<?> calledHeaderType_;
  private Class<?> calledPayloadType_;
  
  @Test
  public void testDispatch()
  {
    IAllegroDecryptor decryptor = new IAllegroDecryptor()
    {
      
//      @Override
//      public IApplicationObjectPayload decryptObject(IStoredApplicationRecord storedApplicationRecord)
//      {
//        if(storedApplicationRecord instanceof TestStoredApplicationRecord)
//        {
//          return ((TestStoredApplicationRecord)storedApplicationRecord).payload_;
//        }
//        return null;
//      }
      
      @Override
      public IApplicationObjectPayload decryptObject(IEncryptedApplicationPayload encryptedApplicationPayload)
      {
        if(encryptedApplicationPayload instanceof TestStoredApplicationRecord)
        {
          return ((TestStoredApplicationRecord)encryptedApplicationPayload).payload_;
        }
        return null;
      }
      
      @Override
      public IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message)
      {
        // TODO Auto-generated method stub
        return null;
      }
    };
    
    StoredRecordConsumerManager consumerManager = new StoredRecordConsumerManager.Builder()
        .withModelRegistry(new ModelRegistry().withFactories(CoreModel.FACTORIES).withFactories(ObjectModel.FACTORIES))
        .withDecryptor(decryptor)
        .withConsumer(newConsumer(Header2.class, Payload1.class).holder())
        .withConsumer(newConsumer(Header1.class, Payload2.class).holder())
        .withConsumer(newConsumer(Header1.class, Payload1.class).holder())
        .withConsumer(newConsumer(null, Payload2.class).holder())
      .build();
    
    
    
    consumerManager.accept(new TestStoredApplicationRecord.Builder()
        .withHeader(new Header2())
        .withPayload(new Payload2())
        .build());

    assertEquals(Header1.class, calledHeaderType_);
    assertEquals(Payload2.class, calledPayloadType_);
    
    consumerManager.accept(new TestStoredApplicationRecord.Builder()
        .withHeader(new Header2())
        .withPayload(new Payload1())
        .build());

    assertEquals(Header2.class, calledHeaderType_);
    assertEquals(Payload1.class, calledPayloadType_);
    
    consumerManager.accept(new TestStoredApplicationRecord.Builder()
        .withHeader(new Header1())
        .withPayload(new Payload1())
        .build());

    assertEquals(Header1.class, calledHeaderType_);
    assertEquals(Payload1.class, calledPayloadType_);

    consumerManager.accept(new TestStoredApplicationRecord.Builder()
        .withHeader(new Header2())
        .withPayload(new Payload2())
        .build());

    assertEquals(Header1.class, calledHeaderType_);
    assertEquals(Payload2.class, calledPayloadType_);
    
    consumerManager.accept(new TestStoredApplicationRecord.Builder()
        .withPayload(new Payload2())
        .build());

    assertEquals(null, calledHeaderType_);
    assertEquals(Payload2.class, calledPayloadType_);
    

    consumerManager.accept(new TestStoredApplicationRecord.Builder()
        .withPayload(new Payload3())
        .build());

    assertEquals(null, calledHeaderType_);
    assertEquals(Payload2.class, calledPayloadType_);
  }
  
  
  
  class TestConsumer<H extends IEntity, P extends IEntity> implements IApplicationRecordConsumer<H, P>
  {
    private Class<H> headerType_;
    private Class<P> payloadType_;

    TestConsumer(Class<H> headerType, Class<P> payloadType)
    {
      headerType_ = headerType;
      payloadType_ = payloadType;
    }
    
    public StoredRecordConsumerHolder<?, ?> holder()
    {
      return new StoredRecordConsumerHolder<H,P>(headerType_, payloadType_, this);
    }

    @Override
    public void accept(IStoredApplicationRecord record, H header, P payload)
    {
      System.out.println("consumer " + 
          (headerType_ == null ? "null" : headerType_.getSimpleName()) + " " + 
          (payloadType_ == null ? "null" : payloadType_.getSimpleName()));
      
      calledHeaderType_ = headerType_;
      calledPayloadType_ = payloadType_;
    }
    
    
  }
  
  <H extends IEntity, P extends IEntity> TestConsumer<H,P> newConsumer(Class<H> headerType, Class<P> payloadType)
  {
    return new TestConsumer<H,P>(headerType, payloadType);
  }
}

class TestStoredApplicationRecord extends StoredApplicationRecord implements IStoredApplicationRecord
{
  public final IApplicationObjectPayload payload_;

  public TestStoredApplicationRecord(Builder builder)
  {
    super(builder);
    payload_ = builder.payload_;
  }

  static class Builder extends StoredApplicationRecord.AbstractStoredApplicationRecordBuilder<Builder, IStoredApplicationRecord>
  {
    private IApplicationObjectPayload payload_;

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    public Builder withPayload(IApplicationObjectPayload payload)
    {
      payload_ = payload;
      withEncryptedPayload(payload.serialize());
      
      return self();
    }

    @Override
    protected IStoredApplicationRecord construct()
    {
      return new TestStoredApplicationRecord(this);
    }
  }
}
