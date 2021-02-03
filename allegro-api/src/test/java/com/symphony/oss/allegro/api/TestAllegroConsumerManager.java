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
import com.symphony.oss.models.core.canon.ApplicationPayload;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.core.canon.facade.ApplicationRecord;
import com.symphony.oss.models.core.canon.facade.EncryptedApplicationRecord;
import com.symphony.oss.models.core.canon.facade.IApplicationRecord;
import com.symphony.oss.models.core.canon.facade.IEncryptedApplicationRecord;
import com.symphony.oss.models.crypto.canon.CryptoModel;

public class TestAllegroConsumerManager
{
  class Header1 extends ApplicationPayload  {

    public Header1()
    {
      super(new ApplicationPayload.Builder());
    }
  }
  class Header2 extends Header1  {}
  class Header3 extends Header2  {}
  
  class Payload1 extends ApplicationPayload  {

    public Payload1()
    {
      super(new ApplicationPayload.Builder());
    }}
  class Payload2 extends Payload1  {}
  class Payload3 extends Payload2  {}
  

  private Class<?> calledHeaderType_;
  private Class<?> calledPayloadType_;
  
  @Test
  public void testDispatch()
  {
    AllegroDecryptor decryptor = new AllegroDecryptor()
    {
      
//      @Override
//      public IApplicationObjectPayload decryptObject(IEncryptedApplicationRecord storedApplicationRecord)
//      {
//        if(storedApplicationRecord instanceof TestStoredApplicationRecord)
//        {
//          return ((TestStoredApplicationRecord)storedApplicationRecord).payload_;
//        }
//        return null;
//      }
      
//      @Override
//      public IApplicationObjectPayload decryptObject(IEncryptedApplicationPayload encryptedApplicationPayload)
//      {
//        if(encryptedApplicationPayload instanceof TestStoredApplicationRecord)
//        {
//          return ((TestStoredApplicationRecord)encryptedApplicationPayload).payload_;
//        }
//        return null;
//      }
      
      @Override
      public IReceivedChatMessage decryptChatMessage(ILiveCurrentMessage message)
      {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      IApplicationRecord decryptObject(IEncryptedApplicationRecord encryptedApplicationRecord)
      {
        if(encryptedApplicationRecord instanceof TestEncryptedApplicationRecord)
        {
          return new TestApplicationRecord.Builder()
              .withHeader(encryptedApplicationRecord.getHeader())
              .withPayload(((TestEncryptedApplicationRecord)encryptedApplicationRecord).payload_)
              .build();
        }
        return null;
      }
    };
    
    AllegroConsumerManager consumerManager = new AllegroConsumerManager.Builder(decryptor,
          new ModelRegistry().withFactories(CoreModel.FACTORIES).withFactories(CryptoModel.FACTORIES))
        .withConsumer(newConsumer(Header2.class, Payload1.class).holder())
        .withConsumer(newConsumer(Header1.class, Payload2.class).holder())
        .withConsumer(newConsumer(Header1.class, Payload1.class).holder())
        .withConsumer(newConsumer(null, Payload2.class).holder())
      .build();
    
    
    
    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withHeader(new Header2())
        .withPayload(new Payload2())
        .build());

    assertEquals(Header1.class, calledHeaderType_);
    assertEquals(Payload2.class, calledPayloadType_);
    
    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withHeader(new Header2())
        .withPayload(new Payload1())
        .build());

    assertEquals(Header2.class, calledHeaderType_);
    assertEquals(Payload1.class, calledPayloadType_);
    
    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withHeader(new Header1())
        .withPayload(new Payload1())
        .build());

    assertEquals(Header1.class, calledHeaderType_);
    assertEquals(Payload1.class, calledPayloadType_);

    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withHeader(new Header2())
        .withPayload(new Payload2())
        .build());

    assertEquals(Header1.class, calledHeaderType_);
    assertEquals(Payload2.class, calledPayloadType_);
    
    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withPayload(new Payload2())
        .build());

    assertEquals(null, calledHeaderType_);
    assertEquals(Payload2.class, calledPayloadType_);
    

    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withPayload(new Payload3())
        .build());

    assertEquals(null, calledHeaderType_);
    assertEquals(Payload2.class, calledPayloadType_);
  }
  
  
  
  class TestConsumer<H extends IApplicationPayload, P extends IApplicationPayload> implements IApplicationRecordConsumer<H, P>
  {
    private Class<H> headerType_;
    private Class<P> payloadType_;

    TestConsumer(Class<H> headerType, Class<P> payloadType)
    {
      headerType_ = headerType;
      payloadType_ = payloadType;
    }
    
    public ApplicationConsumerHolder<?, ?> holder()
    {
      return new ApplicationConsumerHolder<H,P>(headerType_, payloadType_, this);
    }

    @Override
    public void accept(IEncryptedApplicationRecord record, H header, P payload)
    {
      System.out.println("consumer " + 
          (headerType_ == null ? "null" : headerType_.getSimpleName()) + " " + 
          (payloadType_ == null ? "null" : payloadType_.getSimpleName()));
      
      calledHeaderType_ = headerType_;
      calledPayloadType_ = payloadType_;
    }
    
    
  }
  
  <H extends IApplicationPayload, P extends IApplicationPayload> TestConsumer<H,P> newConsumer(Class<H> headerType, Class<P> payloadType)
  {
    return new TestConsumer<H,P>(headerType, payloadType);
  }
}

class TestApplicationRecord extends ApplicationRecord implements IApplicationRecord
{
  public final IApplicationPayload payload_;

  public TestApplicationRecord(Builder builder)
  {
    super(builder);
    payload_ = builder.payload_;
  }

  static class Builder extends ApplicationRecord.AbstractApplicationRecordBuilder<Builder, IApplicationRecord>
  {
    private IApplicationPayload payload_;

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected IApplicationRecord construct()
    {
      return new TestApplicationRecord(this);
    }
  }
}

class TestEncryptedApplicationRecord extends EncryptedApplicationRecord implements IEncryptedApplicationRecord
{
  public final IApplicationPayload payload_;

  public TestEncryptedApplicationRecord(Builder builder)
  {
    super(builder);
    payload_ = builder.payload_;
  }

  static class Builder extends EncryptedApplicationRecord.AbstractEncryptedApplicationRecordBuilder<Builder, IEncryptedApplicationRecord>
  {
    private IApplicationPayload payload_;

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    public Builder withPayload(IApplicationPayload payload)
    {
      payload_ = payload;
      withEncryptedPayload(payload.serialize());
      
      return self();
    }

    @Override
    protected IEncryptedApplicationRecord construct()
    {
      return new TestEncryptedApplicationRecord(this);
    }
  }
}
