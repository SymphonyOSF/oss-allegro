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

package com.symphony.oss.allegro.objectstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.fugue.pipeline.IRetryableConsumer;
import com.symphony.oss.fugue.trace.ITraceContext;

abstract class AbstractAdaptor<T> implements IRetryableConsumer<T>
{
  private static final Logger log_ = LoggerFactory.getLogger(AbstractAdaptor.class);
  
  private final Class<T> payloadType_;
  
  protected IRetryableConsumer<Object>           defaultConsumer_ = new IRetryableConsumer<Object>()
  {
    @Override
    public synchronized void consume(Object item, ITraceContext trace)
    {
      log_.error("No consumer found for message of type " + item.getClass() + "\n" + item);
    }
    
    @Override
    public void close(){}
  };
  
  public AbstractAdaptor(Class<T> payloadType)
  {
    payloadType_ = payloadType;
  }

  public Class<T> getPayloadType()
  {
    return payloadType_;
  }

  public void setDefaultConsumer(IRetryableConsumer<Object> defaultConsumer)
  {
    defaultConsumer_ = defaultConsumer;
  }
}
