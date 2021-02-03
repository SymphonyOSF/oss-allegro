/*
 *
 *
 * Copyright 2020 Symphony Communication Services, LLC.
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.security.PrivateKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.canon.runtime.IEntityFactory;
import com.symphony.oss.canon.runtime.ModelRegistry;
import com.symphony.oss.canon.runtime.jjwt.Rs512JwtGenerator;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.models.allegro.canon.facade.IObjectStoreConfiguration;
import com.symphony.oss.models.allegro.canon.facade.ObjectStoreConfiguration;
import com.symphony.oss.models.core.canon.CoreModel;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.crypto.cipher.CipherSuiteUtils;
import com.symphony.oss.models.object.canon.ObjectModel;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.s2.authc.canon.AuthcModel;
import com.symphony.s2.authc.canon.facade.IPrincipalCredential;
import com.symphony.s2.authc.canon.facade.PrincipalCredential;
import com.symphony.s2.authz.canon.AuthzModel;

/**
 * Implementation of IAllegroMultiTenantApi, the main Allegro Multi Tenant API class.
 * 
 * @author Bruce Skingle
 *
 */
public class ObjectStoreApi extends AllegroBaseApi implements IObjectStoreApi
{
  private static final Logger             log_ = LoggerFactory.getLogger(ObjectStoreApi.class);

  private final IObjectStoreConfiguration config_;
  private final ModelRegistry             modelRegistry_;
  private final PodAndUserId              userId_;
  private final Rs512JwtGenerator         jwtBuilder_;

  
  ObjectStoreApi(AbstractBuilder<?, ?> builder)
  {
    super(builder, "https://api.symphony.com");
    
    config_ = builder.config_;
    
    modelRegistry_ = new ModelRegistry()
        .withFactories(ObjectModel.FACTORIES)
        .withFactories(AuthcModel.FACTORIES)
        .withFactories(AuthzModel.FACTORIES)
        .withFactories(CoreModel.FACTORIES)
        ;
    
    for(IEntityFactory<?, ?, ?> factory : builder.factories_)
      modelRegistry_.withFactories(factory);
    
    log_.info("AllegroMultiTenantApi constructor start with configuredUserId " + builder.configuredUserId_ + " and config " + builder.config_.getRedacted());

    userId_ = builder.configuredUserId_;
    
    jwtBuilder_ = new Rs512JwtGenerator(builder.rsaCredential_)
        .withClaim("userId", String.valueOf(userId_))
        ;
    
    if(builder.keyId_ != null)
      jwtBuilder_.withClaim("kid", builder.keyId_);
  }

  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, IObjectStoreApi>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected IObjectStoreApi construct()
    {
      return new ObjectStoreApi(this);
    }
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
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends IObjectStoreApi>
  extends AllegroBaseApi.AbstractBuilder<T, B>
  {
    protected IObjectStoreConfiguration config_;
    protected PrivateKey                rsaCredential_;
    protected PodAndUserId              configuredUserId_;
    protected String                    keyId_;
   
    public AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    public T withConfiguration(IObjectStoreConfiguration configuration)
    {
      config_ = configuration;
      
      return self();
    }

    @Override
    public T withConfiguration(Reader reader)
    {
      withConfiguration(configModelRegistry_.parseOne(reader, ObjectStoreConfiguration.TYPE_ID, IObjectStoreConfiguration.class));
      
      return self();
    }

    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      if(config_.getPrincipalCredential() != null) {
        
        IPrincipalCredential principalCredential = config_.getPrincipalCredential();
        
        rsaCredential_    = CipherSuiteUtils.privateKeyFromPem(principalCredential.getEncodedPrivateKey());
        keyId_            = principalCredential.getKeyId().toString();
        configuredUserId_ = principalCredential.getUserId();
        
      }
      else if(config_.getPrincipalCredentialFile() != null)
      {
        File file = new File(config_.getPrincipalCredentialFile());
        
        if(!file.canRead())
          throw new IllegalArgumentException("PrincipalCredential file \"" + file.getAbsolutePath() + "\" is unreadable");
        
        try(Reader reader = new FileReader(file))
        {
          IPrincipalCredential principalCredential = configModelRegistry_.parseOne(reader, PrincipalCredential.TYPE_ID, IPrincipalCredential.class);
          
          rsaCredential_    = CipherSuiteUtils.privateKeyFromPem(principalCredential.getEncodedPrivateKey());
          keyId_            = principalCredential.getKeyId().toString();
          configuredUserId_ = principalCredential.getUserId();
        }
        catch (IOException e)
        {
          throw new IllegalArgumentException("Unable to read credential file \""  + file.getAbsolutePath() + "\".", e);
        }
      }
      else
      {
        keyId_ = config_.getKeyId();
        configuredUserId_ = config_.getUserId();
      }
      
      if(rsaCredential_ == null)
      {
        faultAccumulator.error("rsaCredential is required");
      }
      
      faultAccumulator.checkNotNull(configuredUserId_, "User ID");
    }

    @Override
    public IObjectStoreConfiguration getConfiguration()
    {
      return config_;
    }
  }

  @Override
  IApplicationObjectPayload decryptObject(IStoredApplicationObject encryptedApplicationPayload)
  {
    return null;
  }

  @Override
  public IObjectStoreConfiguration getConfiguration()
  {
    return config_;
  }

  @Override
  public PodAndUserId getUserId()
  {
    return userId_;
  }

  @Override
  public String getApiAuthorizationToken()
  {
    return jwtBuilder_.createJwt();
  }
}
