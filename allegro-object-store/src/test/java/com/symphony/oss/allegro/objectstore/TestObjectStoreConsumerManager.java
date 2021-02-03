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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.commons.hash.HashProvider;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.core.canon.IApplicationPayload;
import com.symphony.oss.models.core.canon.facade.ApplicationRecord;
import com.symphony.oss.models.core.canon.facade.IApplicationRecord;
import com.symphony.oss.models.crypto.canon.CryptoModel;
import com.symphony.oss.models.object.canon.facade.ApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.ApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.StoredApplicationObject;

public class TestObjectStoreConsumerManager
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
    ObjectStoreDecryptor decryptor = new ObjectStoreDecryptor()
    {
      @Override
      IApplicationObjectPayload decryptObject(IStoredApplicationObject storedApplicationObject)
      {
        if(storedApplicationObject instanceof TestEncryptedApplicationRecord)
        {
          return (((TestEncryptedApplicationRecord)storedApplicationObject).payload_);
        }
        return null;
      }
    };
    
    ObjectStoreConsumerManager consumerManager = new ObjectStoreConsumerManager.Builder(decryptor,
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
  

  @Test
  public void testNoDecryptDispatch()
  {
    ObjectStoreDecryptor decryptor = new ObjectStoreDecryptor()
    {
      @Override
      IApplicationObjectPayload decryptObject(IStoredApplicationObject storedApplicationObject)
      {
        return null;
      }
    };
    
    ObjectStoreConsumerManager consumerManager = new ObjectStoreConsumerManager.Builder(decryptor,
          new ModelRegistry().withFactories(CoreModel.FACTORIES).withFactories(CryptoModel.FACTORIES))
        .withConsumer(newConsumer(Header2.class, Payload1.class).holder())
        .withConsumer(newConsumer(Header1.class, Payload2.class).holder())
        .withConsumer(newConsumer(Header1.class, null).holder())
        .withConsumer(newConsumer(null, Payload2.class).holder())
        .withConsumer(newConsumer(null, null).holder())
      .build();
    
    
    
    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withHeader(new Header2())
        .withPayload(new Payload2())
        .build());

    assertEquals(Header1.class, calledHeaderType_);
    assertEquals(null, calledPayloadType_);
    
    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withHeader(new Header1())
        .withPayload(new Payload1())
        .build());

    assertEquals(Header1.class, calledHeaderType_);
    assertEquals(null, calledPayloadType_);

    
    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withPayload(new Payload2())
        .build());

    assertEquals(null, calledHeaderType_);
    assertEquals(null, calledPayloadType_);
    

    consumerManager.accept(new TestEncryptedApplicationRecord.Builder()
        .withPayload(new Payload3())
        .build());

    assertEquals(null, calledHeaderType_);
    assertEquals(null, calledPayloadType_);
  }
  
  
  class TestConsumer<H extends IApplicationObjectHeader, P extends IApplicationObjectPayload> implements IApplicationObjectConsumer<H, P>
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
    public void accept(IStoredApplicationObject storedObject, H header, P payload)
    {

      System.out.println("consumer " + 
          (headerType_ == null ? "null" : headerType_.getSimpleName()) + " " + 
          (payloadType_ == null ? "null" : payloadType_.getSimpleName()));
      
      calledHeaderType_ = headerType_;
      calledPayloadType_ = payloadType_;
    }
    
    
  }
  
  <H extends IApplicationObjectHeader, P extends IApplicationObjectPayload> TestConsumer<H,P> newConsumer(Class<H> headerType, Class<P> payloadType)
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

class TestEncryptedApplicationRecord extends StoredApplicationObject implements IStoredApplicationObject
{
  private static final Hash PARTITION_HASH = HashProvider.getCompositeHashOf("PartitionId");
  
  public final IApplicationObjectPayload payload_;

  public TestEncryptedApplicationRecord(Builder builder)
  {
    super(builder);
    payload_ = builder.payload_;
  }

  static class Builder extends StoredApplicationObject.AbstractStoredApplicationObjectBuilder<Builder, IStoredApplicationObject>
  {
    private IApplicationObjectPayload payload_;

    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
      
      withPartitionHash(PARTITION_HASH);
      withSortKey("SortKey");
    }

    public Builder withPayload(IApplicationObjectPayload payload)
    {
      payload_ = payload;
      withEncryptedPayload(payload.serialize());
      
      return self();
    }

    @Override
    protected IStoredApplicationObject construct()
    {
      return new TestEncryptedApplicationRecord(this);
    }
  }
}
