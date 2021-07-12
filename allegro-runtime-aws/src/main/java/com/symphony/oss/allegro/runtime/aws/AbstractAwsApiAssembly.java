/*
 * Copyright 2019 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package com.symphony.oss.allegro.runtime.aws;

import com.symphony.oss.canon.runtime.IHandlerContainer;
import com.symphony.oss.canon.runtime.IHandlerContainerFactory;
import com.symphony.oss.commons.fault.FaultAccumulator;

/**
 * Assembly for assemblies with a KvStore.
 * 
 * @author Bruce Skingle
 *
 */
public class AbstractAwsApiAssembly extends AbstractAwsAssembly
{

  protected final IHandlerContainer                     handlerContainer_;
  
  protected AbstractAwsApiAssembly(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    handlerContainer_ = register(builder.handlerContainerFactory_.createHandlerContainer(traceFactory_, modelRegistry_));
  }
  
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractAwsApiAssembly>
  extends AbstractAwsAssembly.AbstractBuilder<T, B>
  {
    protected IHandlerContainerFactory        handlerContainerFactory_;
 
    public AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    /**
     * Set the factory to create a handler container.
     * 
     * @param handlerContainerFactory the factory to create a handler container.
     * 
     * @return this (fluent method).
     */
    public T withHandlerContainerFactory(IHandlerContainerFactory handlerContainerFactory)
    {
      handlerContainerFactory_ = handlerContainerFactory;
      
      return self();
    }

    public IHandlerContainerFactory getHandlerContainerFactory()
    {
      return handlerContainerFactory_;
    }

    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(handlerContainerFactory_, "handlerContainerFactory");
  
    }
  }
}
