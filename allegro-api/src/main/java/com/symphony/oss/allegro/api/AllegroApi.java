/*
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

import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import com.symphony.oss.allegro.api.request.PartitionId;
import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.commons.dom.json.ImmutableJsonObject;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.pipeline.FatalConsumerException;
import com.symphony.oss.fugue.pipeline.RetryableConsumerException;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.models.allegro.canon.facade.AllegroConfiguration;
import com.symphony.oss.models.allegro.canon.facade.IAllegroConfiguration;
import com.symphony.oss.models.core.canon.HashType;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.RotationId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.EncryptedData;
import com.symphony.oss.models.object.canon.EncryptedApplicationPayloadAndHeader;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayloadAndHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectHeader;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.SortKey;
import com.symphony.oss.models.object.canon.facade.StoredApplicationObject;
import com.symphony.oss.models.pod.canon.IUserV2;
import com.symphony.oss.models.pod.canon.IV2UserList;

/**
 * Implementation of IAllegroApi, the main Allegro API class.
 * 
 * @author Bruce Skingle
 *
 */
public class AllegroApi extends AllegroBaseApi implements IAllegroApi
{
  private final AllegroPodApi                  allegroPodApi_;
  
  /**
   * Constructor.
   * 
   * @param builder The builder containing all initialisation values.
   */
  AllegroApi(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    allegroPodApi_ = new AllegroPodApi(builder.podApiBuilder_);
    
  }

  @Override
  public IAllegroPodApi getAllegroPodApi()
  {
    return allegroPodApi_;
  }

  @Override
  @Deprecated
  public IUserV2 getUserByName(String userName) throws NotFoundException
  {
    return fetchUserByName(userName);
  }

  @Override
  @Deprecated
  public IV2UserList getUsersByName(String ...userNames)
  {
    return fetchUsersByName(userNames);
  }

  @Override
  public void close()
  {
    super.close();
    allegroPodApi_.close();
  }
  
  @Override
  public EncryptedApplicationPayloadAndHeaderBuilder newEncryptedApplicationPayloadAndHeaderBuilder()
  {
    return new EncryptedApplicationPayloadAndHeaderBuilder();
  }

  @Override
  public ApplicationObjectBuilder newApplicationObjectBuilder()
  {
    return new ApplicationObjectBuilder();
  }

  @Override
  public ApplicationObjectUpdater newApplicationObjectUpdater(IStoredApplicationObject existing)
  {
    return new ApplicationObjectUpdater(existing);
  }

  
  /**
   * Builder for an EncryptedApplicationPayload plus header.
   * 
   * @author Bruce Skingle
   *
   */
  public class EncryptedApplicationPayloadAndHeaderBuilder extends BaseEncryptedApplicationPayloadBuilder<EncryptedApplicationPayloadAndHeaderBuilder, IEncryptedApplicationPayloadAndHeader, EncryptedApplicationPayloadAndHeader.Builder>
  {
    EncryptedApplicationPayloadAndHeaderBuilder()
    {
      super(EncryptedApplicationPayloadAndHeaderBuilder.class, new EncryptedApplicationPayloadAndHeader.Builder(), allegroPodApi_.cryptoClient_);
    }

    /**
     * Set the id of the thread with whose content key this object will be encrypted.
     * 
     * @param threadId The id of the thread with whose content key this object will be encrypted.
     * 
     * @return This (fluent method).
     */
    public EncryptedApplicationPayloadAndHeaderBuilder withThreadId(ThreadId threadId)
    {
      builder_.withThreadId(threadId);
      
      return self();
    }

    /**
     * Set the unencrypted header for this object.
     * 
     * @param header The unencrypted header for this object.
     * 
     * @return This (fluent method).
     */
    public EncryptedApplicationPayloadAndHeaderBuilder withHeader(IApplicationObjectHeader header)
    {
      builder_.withHeader(header);
      
      return self();
    }

    @Override
    protected IEncryptedApplicationPayloadAndHeader construct()
    {
      return builder_.build();
    }
  }
  
  /**
   * Super class for AppplicationObject builder and updater.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The concrete type for fluent methods.
   */
  abstract class BaseApplicationObjectBuilder<T extends BaseApplicationObjectBuilder<T>> extends EncryptablePayloadbuilder<T, IStoredApplicationObject>
  {
    protected final StoredApplicationObject.Builder  builder_ = new StoredApplicationObject.Builder();
    
    BaseApplicationObjectBuilder(Class<T> type)
    {
      super(type);
    }
    
    BaseApplicationObjectBuilder(Class<T> type,
        IStoredApplicationObject existing)
    {
      super(type);
      
      builder_.withPartitionHash(existing.getPartitionHash())
        .withSortKey(existing.getSortKey())
        .withOwner(getUserId())
        .withPurgeDate(existing.getPurgeDate())
        .withBaseHash(existing.getBaseHash())
        .withPrevHash(existing.getAbsoluteHash())
        .withPrevSortKey(existing.getSortKey())
        ;
    }

    @Override
    public ThreadId getThreadId()
    {
      return builder_.getThreadId();
    }

    @Override
    T withEncryptedPayload(
        EncryptedData value)
    {
      builder_.withEncryptedPayload(value);
      
      return self();
    }

    @Override
    T withCipherSuiteId(
        CipherSuiteId value)
    {
      builder_.withCipherSuiteId(value);
      
      return self();
    }

    @Override
    T withRotationId(RotationId value)
    {
      builder_.withRotationId(value);
      
      return self();
    }

    /**
     * Set the object payload (which is to be encrypted).
     * 
     * @param payload The object payload (which is to be encrypted).
     * 
     * @return This (fluent method).
     */
    public T withPayload(IApplicationObjectPayload payload)
    {
      payload_ = payload;
      
      return self();
    }

    /**
     * Set the unencrypted header for this object.
     * 
     * @param header The unencrypted header for this object.
     * 
     * @return This (fluent method).
     */
    public T withHeader(IApplicationObjectHeader header)
    {
      builder_.withHeader(header);
      
      return self();
    }

    /**
     * Set the purge date for this object.
     * 
     * @param purgeDate The date after which this object may be deleted by the system.
     * 
     * @return This (fluent method).
     */
    public T withPurgeDate(Instant purgeDate)
    {
      builder_.withPurgeDate(purgeDate);
      
      return self();
    }
    
    @Override
    public ImmutableJsonObject getJsonObject()
    {
      return builder_.getJsonObject();
    }

    @Override
    public String getCanonType()
    {
      return builder_.getCanonType();
    }

    @Override
    public Integer getCanonMajorVersion()
    {
      return builder_.getCanonMajorVersion();
    }

    @Override
    public Integer getCanonMinorVersion()
    {
      return builder_.getCanonMinorVersion();
    }

    @Override
    protected void populateAllFields(List<Object> result)
    {
      builder_.populateAllFields(result);
    }
    
    /**
     * Set the sort key for the object.
     * 
     * @param sortKey The sort key to be attached to this object within its partition.
     * 
     * @return This (fluent method).
     */
    public T withSortKey(SortKey sortKey)
    {
      builder_.withSortKey(sortKey);
      
      return self();
    }
    
    /**
     * Set the sort key for the object.
     * 
     * @param sortKey The sort key to be attached to this object within its partition.
     * 
     * @return This (fluent method).
     */
    public T withSortKey(String sortKey)
    {
      builder_.withSortKey(sortKey);
      
      return self();
    }
    
    @Override
    protected void validate()
    {
      if(builder_.getHashType() == null)
        builder_.withHashType(HashType.newBuilder().build(Hash.getDefaultHashTypeId()));

      builder_.withOwner(getUserId());
      
      if(getThreadId() == null)
      {
        if(payload_ != null || builder_.getEncryptedPayload() != null)
          throw new IllegalStateException("ThreadId is required unless there is no payload.");
      }
      else
      {
        if(payload_ == null)
        {
          if(builder_.getEncryptedPayload() == null)
            throw new IllegalStateException("One of Payload or EncryptedPayload is required.");
        }
        else
        {
          allegroPodApi_.cryptoClient_.encrypt(this);
        }
      }
      
      super.validate();
    }
  }
  
  /**
   * Builder for Application Objects.
   * 
   * @author Bruce Skingle
   *
   */
  public class ApplicationObjectBuilder extends BaseApplicationObjectBuilder<ApplicationObjectBuilder>
  {
    ApplicationObjectBuilder()
    {
      super(ApplicationObjectBuilder.class);
    }

    /**
     * Set the id of the thread with whose content key this object will be encrypted.
     * 
     * @param threadId The id of the thread with whose content key this object will be encrypted.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withThreadId(ThreadId threadId)
    {
      builder_.withThreadId(threadId);
      
      return self();
    }
    
    /**
     * Set the partition key for the object from the given partition.
     * 
     * @param partitionHash The Hash of the partition.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withPartition(Hash partitionHash)
    {
      builder_.withPartitionHash(partitionHash);
      
      return self();
    }
    
    /**
     * Set the partition key for the object from the given partition.
     * 
     * @param partitionId The ID of the partition.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withPartition(PartitionId partitionId)
    {
      builder_.withPartitionHash(partitionId.getId(getUserId()).getHash());
      
      return self();
    }
    
    /**
     * Set the partition key for the object from the given partition.
     * 
     * @param partition A partition object.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectBuilder withPartition(IPartition partition)
    {
      builder_.withPartitionHash(partition.getId().getHash());
      
      return self();
    }
    
//    /**
//     * Set the already encrypted object payload and header.
//     * 
//     * @param payload The encrypted object payload and header.
//     * 
//     * @return This (fluent method).
//     */
//    public ApplicationObjectBuilder withEncryptedPayloadAndHeader(IEncryptedApplicationPayloadAndHeader payload)
//    {
//      withHeader(payload.getHeader());
//
//      return withEncryptedPayload(payload);
//    }
//    
//    /**
//     * Set the already encrypted object payload.
//     * 
//     * @param payload The encrypted object payload.
//     * 
//     * @return This (fluent method).
//     */
//    public ApplicationObjectBuilder withEncryptedPayload(IEncryptedApplicationPayload payload)
//    {
//      withEncryptedPayload(payload.getEncryptedPayload());
//      withRotationId(payload.getRotationId());
//      withCipherSuiteId(payload.getCipherSuiteId());
//      withThreadId(payload.getThreadId());
//      
//      return self();
//    }
    
    @Override
    protected IStoredApplicationObject construct()
    {
      return builder_.build();
    }
  }
  

  
  /**
   * Builder for application type FundamentalObjects which takes an existing ApplicationObject for which a new
   * version is to be created.
   * 
   * @author Bruce Skingle
   *
   */
  public class ApplicationObjectUpdater extends BaseApplicationObjectBuilder<ApplicationObjectUpdater>
  {
    /**
     * Constructor.
     * 
     * @param existing An existing Application Object for which a new version is to be created. 
     */
    public ApplicationObjectUpdater(IStoredApplicationObject existing)
    {
      super(ApplicationObjectUpdater.class, existing);
      
      builder_
          .withPartitionHash(existing.getPartitionHash())
          .withSortKey(existing.getSortKey())
          .withOwner(getUserId())
          .withThreadId(existing.getThreadId())
          .withHeader(existing.getHeader())
          .withPurgeDate(existing.getPurgeDate())
          .withBaseHash(existing.getBaseHash())
          .withPrevHash(existing.getAbsoluteHash())
          .withPrevSortKey(existing.getSortKey())
          ;
    }
    
    /**
     * Set the already encrypted object payload and header.
     * 
     * @param payload The encrypted object payload and header.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectUpdater withEncryptedPayloadAndHeader(IEncryptedApplicationPayloadAndHeader payload)
    {
      withHeader(payload.getHeader());

      return withEncryptedPayload(payload);
    }
    
    /**
     * Set the already encrypted object payload.
     * 
     * @param payload The encrypted object payload.
     * 
     * @return This (fluent method).
     */
    public ApplicationObjectUpdater withEncryptedPayload(IEncryptedApplicationPayload payload)
    {
      if(!builder_.getThreadId().equals(payload.getThreadId()))
        throw new IllegalArgumentException("The threadId of an object cannot be changed. The object being updated has thread ID " + builder_.getThreadId());
      
      withEncryptedPayload(payload.getEncryptedPayload());
      withRotationId(payload.getRotationId());
      withCipherSuiteId(payload.getCipherSuiteId());
      
      return self();
    }

    @Override
    protected IStoredApplicationObject construct()
    {
      return builder_.build();
    }
  }
  

  @Override
  void consume(AbstractConsumerManager consumerManager, Object payload, ITraceContext trace) throws RetryableConsumerException, FatalConsumerException
  {
    consumerManager.consume(payload, trace, this);
  }

  /**
   * The builder implementation.
   * 
   * This is implemented as an abstract class to allow for sub-classing in future.
   * 
   * Any sub-class of AllegroApi would need to implement its own Abstract sub-class of this class
   * and then a concrete Builder class which is itself a sub-class of that.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The type of the concrete Builder
   * @param <B> The type of the built class, some subclass of AllegroApi
   */
  static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends IAllegroApi>
  extends AllegroBaseApi.AbstractBuilder<IAllegroConfiguration,
  AllegroConfiguration.AbstractAllegroConfigurationBuilder<?, IAllegroConfiguration>, T, B>
  {
    AllegroPodApi.Builder podApiBuilder_ = new AllegroPodApi.Builder();
    
    public AbstractBuilder(Class<T> type, AllegroConfiguration.Builder builder)
    {
      super(type, builder);
    }
    
    @Override
    public T withConfiguration(Reader reader)
    {
      return withConfiguration(allegroModelRegistry_.parseOne(reader, AllegroConfiguration.TYPE_ID, IAllegroConfiguration.class));
    }
    
    @Deprecated
    public T withUserName(String serviceAccountName)
    {
      configBuilder_.withUserName(serviceAccountName);
      builderSet_ = true;
      
      return self();
    }
    
    /**
     * Set a fixed session token.
     * 
     * @param sessionToken An externally provided session token.
     * 
     * @return This (fluent method).
     * 
     * @deprecated Use withSessionTokenSupplier instead.
     */
    @Deprecated
    public T withSessionToken(String sessionToken)
    {
      podApiBuilder_.sessionTokenSupplier_ = () -> sessionToken;
      
      return self();
    }
    
    /**
     * Set a fixed key manager token.
     * 
     * @param keymanagerToken An externally provided key manager token.
     * 
     * @return This (fluent method).
     * 
     * @deprecated Use withKeymanagerTokenSupplier instead.
     */
    @Deprecated
    public T withKeymanagerToken(String keymanagerToken)
    {
      podApiBuilder_.keyManagerTokenSupplier_ = () -> keymanagerToken;
      
      return self();
    }

    @Deprecated
    public T withPodUrl(URL podUrl)
    {
      configBuilder_.withPodUrl(podUrl);
      builderSet_ = true;
      
      return self();
    }

    @Deprecated
    public T withPodUrl(String podUrl)
    {
      try
      {
        configBuilder_.withPodUrl(new URL(podUrl));
        builderSet_ = true;
      }
      catch (MalformedURLException e)
      {
        throw new IllegalArgumentException("Invalid podUrl", e);
      }
      
      return self();
    }

    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      podApiBuilder_.config_ = config_;
      podApiBuilder_.rsaCredential_ = rsaCredential_;
      podApiBuilder_.rsaCredentialIsSet_ = true;
    }

    public T withSessionTokenSupplier(
        Supplier<String> sessionTokenSupplier)
    {
      podApiBuilder_.withSessionTokenSupplier(sessionTokenSupplier);
      
      return self();
    }

    public T withKeymanagerTokenSupplier(
        Supplier<String> keymanagerTokenSupplier)
    {
      podApiBuilder_.withKeymanagerTokenSupplier(keymanagerTokenSupplier);
      
      return self();
    }
  }
  
  /**
   * Builder for AllegroApi.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, IAllegroApi>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class, new AllegroConfiguration.Builder());
    }

    @Override
    protected IAllegroApi construct()
    {
      return new AllegroApi(this);
    }
  }
  


  @Override
  public PodAndUserId getUserId()
  {
    return allegroPodApi_.getUserId();
  }

  @Override
  public String getApiAuthorizationToken()
  {
    return allegroPodApi_.getApiAuthorizationToken();
  }
}
