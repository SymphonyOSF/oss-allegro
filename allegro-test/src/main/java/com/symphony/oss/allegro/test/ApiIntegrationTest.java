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

package com.symphony.oss.allegro.test;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.symphonyoss.s2.canon.runtime.exception.BadRequestException;
import org.symphonyoss.s2.canon.runtime.exception.PermissionDeniedException;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.IFugueLifecycleComponent;
import org.symphonyoss.s2.fugue.cmd.CommandLineHandler;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.pipeline.ISimpleConsumer;
import org.symphonyoss.s2.fugue.pipeline.ISimpleThreadSafeConsumer;

import com.symphony.oss.allegro.api.AllegroApi;
import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.Permission;
import com.symphony.oss.allegro.api.ResourcePermissions;
import com.symphony.oss.allegro.api.query.IAllegroQueryManager;
import com.symphony.oss.allegro.api.request.AsyncConsumerManager;
import com.symphony.oss.allegro.api.request.ConsumerManager;
import com.symphony.oss.allegro.api.request.FetchFeedObjectsRequest;
import com.symphony.oss.allegro.api.request.FetchObjectVersionsRequest;
import com.symphony.oss.allegro.api.request.FetchPartitionObjectsRequest;
import com.symphony.oss.allegro.api.request.PartitionId;
import com.symphony.oss.allegro.api.request.PartitionQuery;
import com.symphony.oss.allegro.api.request.UpsertFeedRequest;
import com.symphony.oss.allegro.api.request.UpsertPartitionRequest;
import com.symphony.oss.allegro.api.request.VersionQuery;
import com.symphony.oss.allegro.test.canon.AllegroTestModel;
import com.symphony.oss.allegro.test.canon.ITestItem;
import com.symphony.oss.allegro.test.canon.TestItem;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.core.canon.facade.ThreadId;
import com.symphony.oss.models.object.canon.AffectedUsers;
import com.symphony.oss.models.object.canon.IAffectedUsers;
import com.symphony.oss.models.object.canon.IFeed;
import com.symphony.oss.models.object.canon.facade.IPartition;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.StoredApplicationObject;

public class ApiIntegrationTest extends CommandLineHandler implements Runnable
{
  private static final String TEST_PRIVATE_KEY =
      "-----BEGIN RSA PRIVATE KEY-----\n" + 
      "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\n" + 
      "-----END RSA PRIVATE KEY-----";

  private static final String FEED_NAME         = "ApiIntegrationTestFeed";
  private static final String PARTITION_NAME    = "MyPartition";
  private static final int    TEST_OBJECT_COUNT = 5;
  private static final int    TEST_UPDATE_COUNT = 6;

  private static final String ALLEGRO           = "ALLEGRO_";
  private static final String SERVICE_ACCOUNT   = "SERVICE_ACCOUNT";
  private static final String POD_URL           = "POD_URL";
  private static final String OBJECT_STORE_URL  = "OBJECT_STORE_URL";
  private static final String CREDENTIAL_FILE   = "CREDENTIAL_FILE";
  private static final String THREAD_ID         = "THREAD_ID";
  private static final String OTHER_ACCOUNT     = "OTHER_ACCOUNT";
  private static final String INVALID_ACCOUNT   = "INVALID_ACCOUNT";
  private static final String REPEAT            = "REPEAT";


  private String              serviceAccount_;
  private String              otherAccount_;
  private String              invalidAccount_;
  private String              podUrl_ ;
  private String              objectStoreUrl_;
  private String              credentialFile_;
  private boolean             repeat_;

  private PodAndUserId        userId_ ;
  private PodAndUserId        otherUserId_ ;
  private PodAndUserId        invalidUserId_;
  private ThreadId            threadId_;

  private IAllegroApi   allegroApi_;
  private IAllegroApi   otherUserApi_;
  private IAllegroApi   invalidUserApi_;

  private Hash                baseHash_;
  private Thread              mainThread_ = Thread.currentThread();

  private IFeed feed_;

  private Set<Hash> partitionObjectBaseHashes_ = new HashSet<>();


  /**
   * Constructor.
   */
  public ApiIntegrationTest()
  {
    withFlag('s',   SERVICE_ACCOUNT,  ALLEGRO + SERVICE_ACCOUNT,  String.class,   false, true,  (v) -> serviceAccount_   = v);
    withFlag('p',   POD_URL,          ALLEGRO + POD_URL,          String.class,   false, true,  (v) -> podUrl_           = v);
    withFlag('o',   OBJECT_STORE_URL, ALLEGRO + OBJECT_STORE_URL, String.class,   false, true,  (v) -> objectStoreUrl_   = v);
    withFlag('f',   CREDENTIAL_FILE,  ALLEGRO + CREDENTIAL_FILE,  String.class,   false, true,  (v) -> credentialFile_   = v);
    withFlag('t',   THREAD_ID,        ALLEGRO + THREAD_ID,        String.class,   false, true,  (v) -> threadId_         = ThreadId.newBuilder().build(v));
    withFlag(null,  OTHER_ACCOUNT,    ALLEGRO + OTHER_ACCOUNT,    String.class,   false, true,  (v) -> otherAccount_     = v);
    withFlag(null,  INVALID_ACCOUNT,  ALLEGRO + INVALID_ACCOUNT,  String.class,   false, true,  (v) -> invalidAccount_   = v);
    withFlag('r',   REPEAT,           ALLEGRO + REPEAT,           Boolean.class,  false, false, (v) -> repeat_           = v);
  }
  
  @Override
  public void run()
  { 
    allegroApi_ = new AllegroApi.Builder()
      .withPodUrl(podUrl_)
      .withObjectStoreUrl(objectStoreUrl_)
      .withUserName(serviceAccount_)
      .withRsaPemCredentialFile(credentialFile_)
      .withFactories(AllegroTestModel.FACTORIES)
      .withTrustAllSslCerts()
      .build();
    
    userId_ = allegroApi_.getUserId();
    
    System.out.println("PodId is " + allegroApi_.getPodId());
    System.out.println("UserId is " + userId_);
    
    otherUserApi_ = new AllegroApi.Builder()
        .withPodUrl(podUrl_)
        .withObjectStoreUrl(objectStoreUrl_)
        .withUserName(otherAccount_)
        .withRsaPemCredentialFile(credentialFile_)
        .withFactories(AllegroTestModel.FACTORIES)
        .withTrustAllSslCerts()
        .build();
    
    otherUserId_ = otherUserApi_.getUserId();
    System.out.println("OtherUserId is " + otherUserId_);
    
    invalidUserApi_ = new AllegroApi.Builder()
        .withPodUrl(podUrl_)
        .withObjectStoreUrl(objectStoreUrl_)
        .withUserName(invalidAccount_)
        .withRsaPemCredentialFile(credentialFile_)
        .withFactories(AllegroTestModel.FACTORIES)
        .withTrustAllSslCerts()
        .build();
    
    invalidUserId_ = invalidUserApi_.getUserId();
    System.out.println("invalidUserId is " + invalidUserId_);
    
    if(!repeat_)
    {
      testPartition(false);
      
      upsertPartition();
    }
    
    testPartition(true);
    
    testPermissionReadPartition(otherUserApi_, true);
    testPermissionReadPartition(invalidUserApi_, false);
    
    if(!repeat_)
    {
      for(int i=0 ; i<TEST_OBJECT_COUNT ; i++)
        createItem(i);
    }
    


    
    fetchPartition();
    fetchAsyncPartition();
//
//    upsertFeed();
//    
//    fetchAllFeedItems();
    
    for(int i=0 ; i<TEST_UPDATE_COUNT ; i++)
      updateItems(i);
    
//    fetchUpdatedFeedItems();
    
//    Why do this again?
//    for(int i=0 ; i<TEST_UPDATE_COUNT ; i++)
//      updateItems(i);
    
    // This needs to be after updateItems()
    fetchItems();
    
    fetchAsyncVersions(null);
    
    getVersions(null);
    getVersions(2);
    
    fetchAsyncUpdatedFeedItems();
    
    
    
    System.out.println();
    System.out.println();
    System.out.println("+------------------------+------------------------+------------------------+------------------------+");
    System.out.println("| ALL TESTS COMPLETED OK | ALL TESTS COMPLETED OK | ALL TESTS COMPLETED OK | ALL TESTS COMPLETED OK |");
    System.out.println("+------------------------+------------------------+------------------------+------------------------+");
  }
  
  private void fetchAsyncPartition()
  {
    Set<Hash> remainingBaseHashes = new HashSet<>(partitionObjectBaseHashes_);
    Set<Hash> unknownBaseHashes = new HashSet<>();
    
    IAllegroQueryManager queryManager = allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
        .withQuery(new PartitionQuery.Builder()
            .withName(PARTITION_NAME)
            .build()
            )
        .withConsumerManager(new AsyncConsumerManager.Builder()
            .withSubscriberThreadPoolSize(10)
            .withHandlerThreadPoolSize(90)
            .withConsumer(IStoredApplicationObject.class, (item, trace) ->
            {
              if(!remainingBaseHashes .remove(item.getBaseHash()))
                unknownBaseHashes.add(item.getBaseHash());
            })
            .build()
            )
        .build()
        );
    
    queryManager.start();
    
    try
    {
      queryManager.waitUntilIdle();
    }
    catch (InterruptedException e)
    {
      throw new IllegalStateException("Interrupted while waiting for query.", e);
    }
    
//    System.out.println("Subscriber state: " + subscriber.getLifecycleState());
//    subscriber.start();
//    
//    
//    //TODO: fix this to exit when no more messages....
//    
//    try
//    {
//      Thread.sleep(40000);
//    }
//    catch(InterruptedException e)
//    {
//      throw new IllegalStateException("Sleep interrupted", e);
//    }
    
    if(!remainingBaseHashes.isEmpty())
      throw new IllegalStateException("Did not receive expected objects " + remainingBaseHashes);
    
    if(!unknownBaseHashes.isEmpty())
      throw new IllegalStateException("Received unexpected objects " + unknownBaseHashes);
  }

  private void fetchPartition()
  {
    allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
        .withQuery(new PartitionQuery.Builder()
            .withName(PARTITION_NAME)
            .build()
            )
        .withConsumerManager(new ConsumerManager.Builder()
            .withConsumer(IStoredApplicationObject.class, (item, trace) ->
            {
              partitionObjectBaseHashes_ .add(item.getBaseHash());
            })
            .build()
            )
        .build()
        );
    
    System.out.println("Read " + partitionObjectBaseHashes_.size() + " objects from partition.");
  }

  private void fetchAsyncUpdatedFeedItems()
  {
    VersionConsumer consumer = new VersionConsumer(null, true);
    
    IFugueLifecycleComponent subscriber = allegroApi_.fetchFeedObjects(new FetchFeedObjectsRequest.Builder()
        .withName(FEED_NAME)
        .withConsumerManager(new AsyncConsumerManager.Builder()
            .withSubscriberThreadPoolSize(10)
            .withHandlerThreadPoolSize(90)
            .withConsumer(IStoredApplicationObject.class, consumer)
          .build()
        )
      .build()
    );

    System.out.println("Subscriber state: " + subscriber.getLifecycleState());
    subscriber.start();
    
    
    //TODO: fix this to exit when no more messages....
    
    try
    {
      Thread.sleep(40000);
    }
    catch(InterruptedException e)
    {
      throw new IllegalStateException("Sleep interrupted", e);
    }
    
    int expectedCnt = (repeat_ ? TEST_OBJECT_COUNT - 1 : TEST_OBJECT_COUNT) * TEST_UPDATE_COUNT;
    
    if(consumer.count_ != expectedCnt)
      throw new IllegalStateException("Expected " + expectedCnt + " feed items but got " + consumer.count_);
  }

  private void fetchUpdatedFeedItems()
  {
    int             totalCnt=0;
    VersionConsumer consumer = null;
    
    do
    {
      consumer = new VersionConsumer(null, false);
      
      allegroApi_.fetchFeedObjects(new FetchFeedObjectsRequest.Builder()
          .withName(FEED_NAME)
          .withConsumerManager(new ConsumerManager.Builder()
              .withConsumer(IStoredApplicationObject.class, consumer)
              .build())
          .build()
          );
      
      System.out.println("fetchFeedObjects returned " + consumer.count_ + " items.");
      
      totalCnt += consumer.count_;
      
    }while(consumer.count_ > 0);
    
    int expectedCnt = (repeat_ ? TEST_OBJECT_COUNT - 1 : TEST_OBJECT_COUNT) * TEST_UPDATE_COUNT;
    
    if(totalCnt != expectedCnt)
      throw new IllegalStateException("Expected " + expectedCnt + " feed items but got " + totalCnt);
  }

  private void fetchAllFeedItems()
  {
    VersionConsumer consumer = null;
    
    do
    {
      consumer = new VersionConsumer(null, false);
      
      allegroApi_.fetchFeedObjects(new FetchFeedObjectsRequest.Builder()
          .withName(FEED_NAME)
          .withConsumerManager(new ConsumerManager.Builder()
              .withConsumer(IStoredApplicationObject.class, consumer)
              .build())
          .build()
          );
      
      System.out.println("fetchFeedObjects returned " + consumer.count_ + " items.");
    }while(consumer.count_ > 0);
  }

  private void upsertFeed()
  {
    ResourcePermissions permissions = null;
    
    if(otherUserId_ != null)
    {
      permissions = new ResourcePermissions.Builder()
          .withUser(otherUserId_, Permission.Read)
          .build()
          ;
    }
    
    UpsertFeedRequest.Builder builder = new UpsertFeedRequest.Builder()
        .withName(FEED_NAME)
        .withPermissions(permissions)
        .withPartitionIds(
            new PartitionId.Builder()
            .withName(PARTITION_NAME)
            .build()
            )
        ;
    
    feed_ = allegroApi_.upsertFeed(builder.build());
    
    System.out.println("Feed is " + feed_);
  }

  private void fetchItems()
  {
    allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
        .withQuery(new PartitionQuery.Builder()
            .withName(PARTITION_NAME)
            .build()
            )
        .withConsumerManager(new ConsumerManager.Builder()
            .withMaxItems(10)
            .withConsumer(IStoredApplicationObject.class, (item, trace) ->
            {              
              if(baseHash_ == null)
                baseHash_ = item.getBaseHash();
            })
            .build()
            )
        .build()
        );
  }

  private void testPartition(boolean shouldExist)
  {
    try
    {
      allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
          .withQuery(new PartitionQuery.Builder()
              .withName(PARTITION_NAME)
              .build()
              )
          .withConsumerManager(new ConsumerManager.Builder()
              .withMaxItems(10)
              .withConsumer(ITestItem.class, (item, trace) ->
              {
                System.out.println("Object:  " + item.getStoredApplicationObject().getAbsoluteHash());
              })
              .build()
              )
          .build()
          );
      
      if(!shouldExist)
        throw new IllegalStateException("Partition should not exist, restart inmemory object server.");
    }
    catch(BadRequestException e)
    {
      if(shouldExist)
        throw new IllegalStateException("Partition should exist.", e);
    }
  }

  private void getVersions(Integer maxItems)
  {
    System.out.format("  %-50s %-50s %-50s %-50s %s%n",
        "BaseHash",
        "AbsoluteHash",
        "PartitionHash",
        "SortKey",
        "CreatedDate"
        );
    
    VersionConsumer consumer = new VersionConsumer(baseHash_, false);
    
    allegroApi_.fetchObjectVersions(new FetchObjectVersionsRequest.Builder()
        .withQuery(new VersionQuery.Builder()
            .withBaseHash(baseHash_)
            .build()
            )
        .withConsumerManager(new ConsumerManager.Builder()
            .withMaxItems(maxItems)
            .withConsumer(IStoredApplicationObject.class, consumer)
            .build()
            )
        .build()
        );
    
    int expectedCount = maxItems == null ? TEST_UPDATE_COUNT + 1 : maxItems;
    
    if(consumer.count_ != expectedCount)
      throw new IllegalStateException("Expected " + expectedCount + " object versions but found " + consumer.count_);
  }

  private void fetchAsyncVersions(Integer maxItems)
  {
    System.out.format("  %-50s %-50s %-50s %-50s %s%n",
        "BaseHash",
        "AbsoluteHash",
        "PartitionHash",
        "SortKey",
        "CreatedDate"
        );
    
    VersionConsumer consumer = new VersionConsumer(baseHash_, true);
    
    IAllegroQueryManager queryManager = allegroApi_.fetchObjectVersions(new FetchObjectVersionsRequest.Builder()
        .withQuery(new VersionQuery.Builder()
            .withBaseHash(baseHash_)
            .withMaxItems(maxItems)
            .build()
            )
        .withConsumerManager(new AsyncConsumerManager.Builder()
            .withConsumer(IStoredApplicationObject.class, consumer)
            .build()
            )
        .build()
        );
    
    queryManager.start();
    
    try
    {
      queryManager.waitUntilIdle();
    }
    catch (InterruptedException e)
    {
      throw new IllegalStateException("Interrupted while waiting for query.", e);
    }
    
    int expectedCount = maxItems == null ? TEST_UPDATE_COUNT + 1 : maxItems;
    
    if(consumer.count_ != expectedCount)
      throw new IllegalStateException("Expected " + expectedCount + " object versions but found " + consumer.count_);
  }
  
  private class VersionConsumer implements ISimpleThreadSafeConsumer<IStoredApplicationObject>
  {
    int count_;
    private Hash expectedBaseHash_;
    private boolean async_;

    public VersionConsumer(Hash baseHash, boolean async)
    {
      expectedBaseHash_ = baseHash;
      async_ = async;
    }

    @Override
    public synchronized void consume(IStoredApplicationObject item, ITraceContext trace)
    {
      if(expectedBaseHash_ != null && !item.getBaseHash().equals(expectedBaseHash_))
        throw new IllegalStateException("Expected baseHash " + expectedBaseHash_ + " but received " + item.getBaseHash());
      
      if(async_)
      {
        if(mainThread_ == Thread.currentThread())
          throw new IllegalStateException("Expected to be called from a pool thread but actually called from " + Thread.currentThread());
      }
      else
      {
        if(mainThread_ != Thread.currentThread())
          throw new IllegalStateException("Expected to be called from main thread but actually called from " + Thread.currentThread());
      }

      count_++;
      
      System.out.format("  %-50s %-50s %-50s %-50s %s%n",
          item.getBaseHash(),
          item.getAbsoluteHash(),
          item.getPartitionHash(),
          item.getSortKey(),
          item.getCreatedDate()
          );
    }
    
  }

  private void updateItems(int updateCount)
  {
    UpdateConsumer consumer = new UpdateConsumer(updateCount);
    
    allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
        .withQuery(new PartitionQuery.Builder()
            .withName(PARTITION_NAME)
            .build()
            )
        .withConsumerManager(new ConsumerManager.Builder()
            .withMaxItems(10)
            .withConsumer(ITestItem.class, consumer)
            .build()
            )
        .build()
        );
    
    if(consumer.count_ != TEST_OBJECT_COUNT)
      throw new IllegalStateException("Expected " + TEST_OBJECT_COUNT + " objects but found " + consumer.count_);
  }
  
  private class UpdateConsumer implements ISimpleConsumer<ITestItem>
  {
    private final int updateCount_;
    
    int count_;
    
    public UpdateConsumer(int updateCount)
    {
      updateCount_ = updateCount;
    }

    @Override
    public void consume(ITestItem item, ITraceContext trace)
    {
      count_++;
      
      System.out.println("Header:  " + item.getStoredApplicationObject().getHeader());
      System.out.println("Payload: " + item);
      System.out.println("Stored:  " + item.getStoredApplicationObject());
      
      if(!repeat_ || count_ > 1)
        update(item, updateCount_);
      
      if(mainThread_ != Thread.currentThread())
        throw new IllegalStateException("Expected to be called from main thread but actually called from " + Thread.currentThread());
    }
    
  }
  
  private void update(ITestItem item, int updateCount)
  {
    
    ITestItem TestItem = new TestItem.Builder(item)
        .withDescription("Updated at " + Instant.now() + ", item " + updateCount)
        .build();
      
    System.out.println("About to update item " + TestItem);
    
    StoredApplicationObject testObject = allegroApi_.newApplicationObjectUpdater(item)
        .withPayload(TestItem)
      .build();
    
    allegroApi_.store(testObject);
  }

  private void testPermissionReadPartition(IAllegroApi allegroApi, boolean shouldBeAllowed)
  {
    try
    {
      allegroApi.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
          .withQuery(new PartitionQuery.Builder()
              .withName(PARTITION_NAME)
              .withOwner(userId_)
              .build()
              )
        .withConsumerManager(new ConsumerManager.Builder()
            .withMaxItems(10)
            .build())
        .build()
        );
    
      if(!shouldBeAllowed)
        throw new IllegalStateException("Read operation should fail");
    }
    catch(PermissionDeniedException e)
    {
      if(shouldBeAllowed)
        throw new IllegalStateException("Read operation should succeed.", e);
    }
  }

  private void createItem(int id)
  {
    ITestItem item = new TestItem.Builder()
        .withDue(Instant.now())
        .withTitle("A test Item")
        .withDescription("Item " + id)
        .build();
    
    System.out.println("About to create item " + item);
    
    IAffectedUsers affectedUsers = new AffectedUsers.Builder()
        .withRequestingUser(allegroApi_.getUserId())
        .withAffectedUsers(allegroApi_.getUserId())
        .withEffectiveDate(Instant.now())
        .build();
    
    IStoredApplicationObject toDoObject = allegroApi_.newApplicationObjectBuilder()
        .withThreadId(threadId_)
        .withHeader(affectedUsers)
        .withPayload(item)
        .withPartition(new PartitionId.Builder()
            .withName(PARTITION_NAME)
            .build()
            )
        .withSortKey(Instant.now().toString())
      .build();
    
    allegroApi_.store(toDoObject);
    
    System.out.println("Created " + toDoObject);
  }

  private void upsertPartition()
  {
    ResourcePermissions permissions = null;
    
    if(otherUserId_ != null)
    {
      permissions = new ResourcePermissions.Builder()
          .withUser(otherUserId_, Permission.Read, Permission.Write)
          .build()
          ;
    }
    
    IPartition partition = allegroApi_.upsertPartition(new UpsertPartitionRequest.Builder()
          .withName(PARTITION_NAME)
          .withPermissions(permissions)
          .build()
        );
    
    System.out.println("upserted partition " + partition);
  }

  /**
   * Main.
   * 
   * @param args Command line arguments.
   */
  public static void main(String[] args)
  {
    ApiIntegrationTest program = new ApiIntegrationTest();
    
    program.process(args);
    program.run();
  }
}
