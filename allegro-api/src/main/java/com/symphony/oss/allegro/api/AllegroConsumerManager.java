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

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.allegro.api.StoredRecordConsumerManager.AbstractBuilder;
import com.symphony.oss.allegro.api.StoredRecordConsumerManager.Builder;
import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.core.canon.IApplicationPayload;

public class AllegroConsumerManager
{
  private static final Logger log_ = LoggerFactory.getLogger(AllegroConsumerManager.class);

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
    private final AllegroDecryptor                       allegroDecryptor_;
    private final IModelRegistry                         modelRegistry_;
    private final List<ApplicationRecordConsumerHolder<?, ?>> consumers_     = new LinkedList<>();
    private IErrorConsumer                               errorConsumer_ = new IErrorConsumer()
    {
      @Override
      public void accept(Object item, String message, Throwable cause)
      {
        log_.error("Failed to process object of type " + item.getClass() + ": " + message + "\n" + item, cause);
      }
    };
    
    AbstractBuilder(Class<T> type, AllegroDecryptor allegroDecryptor, IModelRegistry modelRegistry)
    {
      super(type);
      
      allegroDecryptor_ = allegroDecryptor;
      modelRegistry_    = modelRegistry;
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
    
    public T withErrorConsumer(IErrorConsumer errorConsumer)
    {
      errorConsumer_ = errorConsumer;
      
      return self();
    }
    
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkNotNull(modelRegistry_,     "Model Registry");
      faultAccumulator.checkNotNull(allegroDecryptor_,  "Decryptor");
      faultAccumulator.checkNotNull(errorConsumer_,     "ErrorConsumer must not be set to null (there is a default, you don't have to set one)");
    }
  }
  
  public static class Builder extends AbstractBuilder<Builder, AllegroConsumerManager>
  {
    public Builder(AllegroDecryptor allegroDecryptor, IModelRegistry modelRegistry)
    {
      super(Builder.class, allegroDecryptor, modelRegistry);
    }

    @Override
    protected AllegroConsumerManager construct()
    {
      return new AllegroConsumerManager(this);
    }
  }
}
