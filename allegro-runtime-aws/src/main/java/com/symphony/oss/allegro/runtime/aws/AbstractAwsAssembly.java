/*
 * Copyright 2020 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package com.symphony.oss.allegro.runtime.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.symphony.oss.canon.runtime.IEntityFactory;
import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.fugue.aws.assembly.AwsFugueAssembly;
import com.symphony.oss.fugue.aws.kv.table.DynamoDbKvTable;
import com.symphony.oss.fugue.aws.kv.table.DynamoDbKvTable.Builder;
import com.symphony.oss.fugue.aws.kv.table.S3DynamoDbKvTable;
import com.symphony.oss.fugue.kv.table.IKvTable;
import com.symphony.oss.fugue.trace.ITraceContextTransactionFactory;
import com.symphony.oss.fugue.trace.log.LoggerTraceContextTransactionFactory;
import com.symphony.oss.models.core.kv.store.KvStore;

/**
 * Assembly for assemblies with a KvStore.
 * 
 * @author Bruce Skingle
 *
 */
public class AbstractAwsAssembly extends AwsFugueAssembly
{
  protected final ITraceContextTransactionFactory traceFactory_;
  protected final IModelRegistry                  modelRegistry_;
  protected final IKvTable                        dynamoDbKvTable_;
  protected final KvStore                         kvStore_;
  
  protected AbstractAwsAssembly(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    traceFactory_   = builder.traceFactory_==null ? new LoggerTraceContextTransactionFactory() : builder.traceFactory_;
    modelRegistry_  = builder.modelRegistry_;
    
    if(builder.enableStorage_)
    {
      if(builder.enableSecondaryStorage_)
      {
         com.symphony.oss.fugue.aws.kv.table.S3DynamoDbKvTable.Builder kvBuilder = new S3DynamoDbKvTable.Builder()
            .withDeferSecondaryStorage(builder.deferSecondaryStorage_)
            .withConfig(config_)
            .withNameFactory(nameFactory_)
            .withRegion(config_.getCloudRegionId())
            .withServiceId(config_.getServiceId());
        
        if(builder.payloadLimit_ != null)
          kvBuilder.withPayloadLimit(builder.payloadLimit_);
          
        dynamoDbKvTable_ = kvBuilder.build();
      }
      else
      {
           Builder kvBuilder = new DynamoDbKvTable.Builder()
            .withConfig(config_)
            .withNameFactory(nameFactory_)
            .withRegion(config_.getCloudRegionId())
            .withServiceId(config_.getServiceId());
                     
            if(builder.payloadLimit_ != null)
              kvBuilder.withPayloadLimit(builder.payloadLimit_);
              
            dynamoDbKvTable_ = kvBuilder.build();
      }
      
      kvStore_ = new KvStore(dynamoDbKvTable_, modelRegistry_);
    }
    else
    {
      dynamoDbKvTable_ = null;
      kvStore_ = null;
    }
  }
  
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractAwsAssembly>
  extends AwsFugueAssembly.AbstractBuilder<T, B>
  {
    protected final ModelRegistry             modelRegistry_          = new ModelRegistry();
    protected AWSCredentialsProvider          credentials_;
    protected ITraceContextTransactionFactory traceFactory_;
    protected String                          roleName_;
    protected boolean                         enableStorage_ = true;
    protected boolean                         enableSecondaryStorage_;
    protected Integer                         payloadLimit_;
    protected boolean                         deferSecondaryStorage_;

    public AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    public T withDeferSecondaryStorage(boolean deferSecondaryStorage)
    {
      deferSecondaryStorage_ = deferSecondaryStorage;
      
      return self();
    }

    public T withEnableStorage(boolean enableStorage)
    {
      enableStorage_ = enableStorage;
      
      return self();
    }

    public T withEnableSecondaryStorage(boolean enableSecondaryStorage)
    {
      enableSecondaryStorage_ = enableSecondaryStorage;
      
      return self();
    }

    public T withPayloadLimit(int payloadLimit)
    {
      payloadLimit_ = payloadLimit;
      
      return self();
    }
    
    public T withAssumeRole(String roleName)
    {
      roleName_ = roleName;
      
      return self();
    }
    
    public T withCredentials(AWSCredentialsProvider credentials)
    {
      credentials_ = credentials;
      
      return self();
    }
    
    public T withTraceContextTransactionFactory(ITraceContextTransactionFactory traceFactory)
    {
      traceFactory_ = traceFactory;
      
      return self();
    }

    public AWSCredentialsProvider getCredentials()
    {
      return credentials_;
    }

    public T withModelFactories(IEntityFactory<?, ?, ?> ...factories)
    {
      modelRegistry_.withFactories(factories);
      
      return self();
    }
    
    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkValueCount("roleName and credentials", 0, 1, roleName_, credentials_);
      
      if(roleName_ != null)
      {
        withCredentials(stsManager_.assumeRole(roleName_));
      }
    }
  }
}
