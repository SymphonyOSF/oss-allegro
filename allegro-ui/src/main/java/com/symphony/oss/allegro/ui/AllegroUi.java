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

package com.symphony.oss.allegro.ui;

import java.net.URL;
import java.security.PrivateKey;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.allegro.api.AllegroApi;
import com.symphony.oss.allegro.api.AllegroMultiTenantApi;
import com.symphony.oss.allegro.api.AsyncConsumerManager;
import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.IAllegroMultiTenantApi;
import com.symphony.oss.allegro.api.IAllegroQueryManager;
import com.symphony.oss.allegro.api.request.FeedQuery;
import com.symphony.oss.allegro.api.request.FetchFeedObjectsRequest;
import com.symphony.oss.allegro.api.request.UpsertFeedRequest;
import com.symphony.oss.canon.runtime.IEntityFactory;
import com.symphony.oss.commons.fault.FaultAccumulator;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.fugue.cmd.AbstractFlag;
import com.symphony.oss.fugue.cmd.CommandLineHandler;
import com.symphony.oss.fugue.server.http.IResourceProvider;
import com.symphony.oss.fugue.server.http.resources.FugueResourceProvider;
import com.symphony.oss.fugue.server.http.ui.FugueHttpUiServer;
import com.symphony.oss.fugue.trace.ITraceContextTransactionFactory;
import com.symphony.oss.models.crypto.canon.PemPrivateKey;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IFeed;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;

/**
 * Main class for the Allegro UI.
 * 
 * @author Bruce Skingle
 *
 */
public class AllegroUi
{
  private static final Logger log_ = LoggerFactory.getLogger(AllegroUi.class);
  
  private static final String                ALLEGRO          = "ALLEGRO_";
  private static final String                SERVICE_ACCOUNT  = "SERVICE_ACCOUNT";
  private static final String                POD_URL          = "POD_URL";
  private static final String                OBJECT_STORE_URL = "OBJECT_STORE_URL";
  private static final String                PRIVATE_KEY_FILE = "PRIVATE_KEY_FILE";
  private static final String                CREDENTIAL_FILE  = "CREDENTIAL_FILE";
  private static final String                INSTANCE_ID      = "INSTANCE_ID";

  private final String                       instanceId_;

  private final FugueHttpUiServer            httpServer_;
  private final IAllegroApi                  userApi_;
  private final IAllegroMultiTenantApi       accessApi_;
  private final PartitionProvider            partitionProvider_;
  private final PartitionObjectsViewProvider partitionObjectsViewProvider_;
  private final ObjectVersionsViewProvider   objectVersionsViewProvider_;
  private final IFeed                        feed_;
  private final IAllegroQueryManager         feedSubscriber_;
  
  protected AllegroUi(AbstractBuilder<?,?> builder)
  {
    instanceId_ = builder.instanceId_;
  
    userApi_ = builder.userApiBuilder_.build();
  
    if (builder.principalCredentialFile_ == null)
    {
      accessApi_ = userApi_;
    }
    else
    {
      accessApi_ = builder.accessApiBuilder_.build();
    }
  
    partitionProvider_ = new PartitionProvider(accessApi_);
    partitionObjectsViewProvider_ = new PartitionObjectsViewProvider(accessApi_, userApi_);
    objectVersionsViewProvider_ = new ObjectVersionsViewProvider(accessApi_, userApi_);
  
    ProjectorManager projectorManager = builder.projectorBuilder_.build();
  
    ObjectExplorerPanel objectExplorerPanel = new ObjectExplorerPanel(projectorManager, accessApi_, userApi_);
    PartitionBlotterPanel partitionBlotterPanel = null;
  
    if (instanceId_ == null)
    {
      feed_ = null;
      feedSubscriber_ = null;
    }
    else
    {
      String feedName = ALLEGRO + instanceId_;
  
      feed_ = userApi_.upsertFeed(new UpsertFeedRequest.Builder().withName(feedName).build());
  
      partitionBlotterPanel = new PartitionBlotterPanel(projectorManager, feedName, partitionProvider_,
          partitionObjectsViewProvider_, accessApi_, userApi_);
  
      feedSubscriber_ = userApi_.fetchFeedObjects(
          new FetchFeedObjectsRequest.Builder().withQuery(new FeedQuery.Builder().withName(feedName).build())
              .withConsumerManager(
                  new AsyncConsumerManager.Builder().withSubscriberThreadPoolSize(10).withHandlerThreadPoolSize(90)
                      .withConsumer(IAbstractStoredApplicationObject.class, (abstractStoredObject, traceContext) ->
                      {
                        System.err.println("IAbstractStoredApplicationObject " + abstractStoredObject.toString());
                        partitionObjectsViewProvider_.accept(abstractStoredObject);
                      }).withUnprocessableMessageConsumer((item, trace, message, cause) ->
                      {
                        log_.error("Failed to consume message: " + message + "\nPayload:" + item, cause);
                      }).build())
              .build());
  
      feedSubscriber_.start();
    }
  
    FugueHttpUiServer.Builder serverBuilder = new FugueHttpUiServer.Builder()
        .withLocalWebLogin()
        .withApplicationName(builder.applicationName_)
        .withHttpPort(builder.httpPort_)
        .withResourceProvider(builder.resourceProvider_ == null
          ? new FugueResourceProvider()
          : builder.resourceProvider_
        )
        .withApplicationName(builder.applicationName_)
        .withDefaultPanel(new PartitionExplorerPanel(projectorManager, partitionBlotterPanel, partitionProvider_,
            partitionObjectsViewProvider_, accessApi_, userApi_))
        .withPanel(objectExplorerPanel)
        .withPanel(new ObjectVersionsPanel(projectorManager, objectVersionsViewProvider_, accessApi_, userApi_))
        .withPanel(new PodPanel(accessApi_, userApi_)).withLocalWebLogin();
  
    if (partitionBlotterPanel != null)
      serverBuilder.withPanel(partitionBlotterPanel);
    
    httpServer_ = serverBuilder.build();
  }
  
  /**
   * Return the HTTP server.
   * 
   * @return the HTTP server.
   */
  public FugueHttpUiServer getHttpServer()
  {
    return httpServer_;
  }
  
  
  /**
   * The builder implementation.
   * 
   * Any sub-class would need to implement its own Abstract sub-class of this class
   * and then a concrete Builder class which is itself a sub-class of that.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The type of the concrete Builder
   * @param <B> The type of the built class, some subclass of FugueLifecycleComponent
   */
  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AllegroUi>
  extends BaseAbstractBuilder<T, B>
  {
    protected FugueHttpUiServer.Builder     httpServerBuilder_  = new FugueHttpUiServer.Builder();
    protected AllegroApi.Builder            userApiBuilder_     = new AllegroApi.Builder();
    protected AllegroMultiTenantApi.Builder accessApiBuilder_   = new AllegroMultiTenantApi.Builder();
    protected ProjectorManager.Builder      projectorBuilder_   = new ProjectorManager.Builder();
    protected IResourceProvider             resourceProvider_;
    protected String                        principalCredentialFile_;
    protected String                        instanceId_;
    protected String[]                      argv_;
    protected String                        applicationName_;
    protected int                           httpPort_;
    
    protected CommandLineHandler commandLineHandler_ = new CommandLineHandler()
        .withFlag('p',   POD_URL,          ALLEGRO + POD_URL,          String.class,   false, false,  (v) -> withPodUrl(v))
        .withFlag('o',   OBJECT_STORE_URL, ALLEGRO + OBJECT_STORE_URL, String.class,   false, false,  (v) -> withObjectStoreUrl(v))
        .withFlag('s',   SERVICE_ACCOUNT,  ALLEGRO + SERVICE_ACCOUNT,  String.class,   false, false,  (v) -> withUserName(v))
        .withFlag('k',   PRIVATE_KEY_FILE, ALLEGRO + PRIVATE_KEY_FILE, String.class,   false, false,  (v) -> withRsaPemCredentialFile(v))
        .withFlag('c',   CREDENTIAL_FILE,  ALLEGRO + CREDENTIAL_FILE,  String.class,   false, false,  (v) -> withPrincipalCredentialFile(v))
        .withFlag('i',   INSTANCE_ID,      ALLEGRO + INSTANCE_ID,      String.class,   false, false,  (v) -> instanceId_ = v)
        ;
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    public T withCommandLine(String[] argv)
    {
      argv_ = argv;
      
      return self();
    }
    
    public T withInstanceId(String instanceId)
    {
      instanceId_ = instanceId;
      
      return self();
    }
  
    public T withMaxHttpConnections(int maxHttpConnections)
    {
      userApiBuilder_.withMaxHttpConnections(maxHttpConnections);
      accessApiBuilder_.withMaxHttpConnections(maxHttpConnections);
      
      return self();
    }
    
    public T withPrincipalCredentialFile(String principalCredentialFile)
    {
      principalCredentialFile_ = principalCredentialFile;
      
      accessApiBuilder_.withPrincipalCredentialFile(principalCredentialFile);
  
      return self();
    }
  
    public T withTraceFactory(ITraceContextTransactionFactory traceFactory)
    {
      userApiBuilder_.withTraceFactory(traceFactory);
      accessApiBuilder_.withTraceFactory(traceFactory);
      
      return self();
    }
  
    public T withRsaPemCredential(PemPrivateKey rsaPemCredential)
    {
      userApiBuilder_.withRsaPemCredential(rsaPemCredential);
  
      return self();
    }
  
    public T withRsaPemCredential(String rsaPemCredential)
    {
      userApiBuilder_.withRsaPemCredential(rsaPemCredential);
  
      return self();
    }
  
    public T withRsaCredential(PrivateKey rsaCredential)
    {
      userApiBuilder_.withRsaCredential(rsaCredential);
  
      return self();
    }
  
    public T withRsaPemCredentialFile(String rsaPemCredentialFile)
    {
      userApiBuilder_.withRsaPemCredentialFile(rsaPemCredentialFile);
  
      return self();
    }
  
    public T withObjectStoreUrl(URL objectStoreUrl)
    {
      userApiBuilder_.withObjectStoreUrl(objectStoreUrl);
      accessApiBuilder_.withObjectStoreUrl(objectStoreUrl);
  
      return self();
    }
  
    public T withObjectStoreUrl(String objectStoreUrl)
    {
      userApiBuilder_.withObjectStoreUrl(objectStoreUrl);
      accessApiBuilder_.withObjectStoreUrl(objectStoreUrl);
  
      return self();
    }
  
    public T withTrustAllSslCerts()
    {
      userApiBuilder_.withTrustAllSslCerts();
      accessApiBuilder_.withTrustAllSslCerts();
  
      return self();
    }
  
    public T withTrustSelfSignedSslCerts()
    {
      userApiBuilder_.withTrustSelfSignedSslCerts();
      accessApiBuilder_.withTrustSelfSignedSslCerts();
  
      return self();
    }
  
    public T withTrustedSslCertResources(String... resourceNames)
    {
      userApiBuilder_.withTrustedSslCertResources(resourceNames);
      accessApiBuilder_.withTrustedSslCertResources(resourceNames);
  
      return self();
    }
  
    public T withFactories(IEntityFactory<?, ?, ?>... factories)
    {
      userApiBuilder_.withFactories(factories);
  
      return self();
    }
  
    public T withUserName(String serviceAccountName)
    {
      userApiBuilder_.withUserName(serviceAccountName);
  
      return self();
    }
  
    public T withSessionToken(String sessionToken)
    {
      userApiBuilder_.withSessionToken(sessionToken);
  
      return self();
    }
  
    public T withKeymanagerToken(String keymanagerToken)
    {
      userApiBuilder_.withKeymanagerToken(keymanagerToken);
  
      return self();
    }
  
    public T withPodUrl(URL podUrl)
    {
      userApiBuilder_.withPodUrl(podUrl);
  
      return self();
    }
  
    public T withPodUrl(String podUrl)
    {
      userApiBuilder_.withPodUrl(podUrl);
  
      return self();
    }
  
    public <F> T withFlag(Character shortFlag, String longFlag, String envName, Class<F> type,
        boolean multiple, boolean required, Consumer<F> setter)
    {
      commandLineHandler_.withFlag(shortFlag, longFlag, envName, type, multiple, required, setter);
  
      return self();
    }
  
    public T withFlag(AbstractFlag flag)
    {
      commandLineHandler_.withFlag(flag);
  
      return self();
    }
  
    public T withParam(Consumer<String> setter)
    {
      commandLineHandler_.withParam(setter);
  
      return self();
    }
  
    public T withResourceProvider(IResourceProvider provider)
    {
      resourceProvider_ = provider;
  
      return self();
    }
  
    public <P extends IApplicationObjectPayload> T withProjector(Class<P> type, IProjectionEnricher<P> projector)
    {
      projectorBuilder_.withProjector(type, projector);
  
      return self();
    }
  
    public <C, P extends Projection> IProjector<C, P> addProjector(Class<C> type, IProjector<C, P> projector)
    {
      return projectorBuilder_.addProjector(type, projector);
    }
  
    public T withDefaultProjector(IProjector<Object, ?> defaultProjector)
    {
      projectorBuilder_.withDefaultProjector(defaultProjector);
  
      return self();
    }
  
    public T withApplicationName(String applicationName)
    {
      applicationName_ = applicationName;
  
      return self();
    }
  
    public T withHttpPort(int httpPort)
    {
      httpPort_ = httpPort;
  
      return self();
    }
  
    @Override
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      if(argv_ != null)
        commandLineHandler_.process(argv_);
    }
    
  }
  
  static class Builder extends AbstractBuilder<Builder, AllegroUi>
  {
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected AllegroUi construct()
    {
      return new AllegroUi(this);
    }
  }
  
  /**
   * Main.
   * 
   * @param argv Command line arguments.
   */
  public static void main(String[] argv)
  {
    try
    {
      FugueHttpUiServer server = new AllegroUi.Builder()
          .withCommandLine(argv)
          .build()
          .getHttpServer()
          ;
      
      server.start();
  
      try
      {
        server.join();
      }
      finally
      {
        server.stop();
      }
    }
    catch (RuntimeException | InterruptedException e)
    {
      log_.error("Failed", e);
      System.exit(1);
    }
  }
}
