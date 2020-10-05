# oss-allegro
Allegro API for Symphony

For documentation, see [https://allegro.oss.symphony.com/](https://allegro.oss.symphony.com/)

For JavaDocs, see [https://javadoc.io/doc/com.symphony.oss.allegro/allegro-api/latest/index.html](https://javadoc.io/doc/com.symphony.oss.allegro/allegro-api/latest/index.html)

# Change Log

## 2020-10-05 The proxy configuration is now added to the connection configuration of the SQS client

## 2020-07-21 Added the possibility to configure proxy authentication parameters
A proxy can be configured, for which, a hostname and port have to be provided
Optionally, if needed, proxy username and password can be set.

For example:

```java
    allegroApi_ = new AllegroApi.Builder()
            .withConfiguration(new AllegroConfiguration.Builder()
                    .withPodUrl(podUrl_)
                    .withApiUrl(objectStoreUrl_)
                    .withUserName(serviceAccount_)
                    .withRsaPemCredentialFile(credentialFile_)
                    .withApiConnectionSettings(new ConnectionSettings.Builder()
                        .withSslTrustStrategy(SslTrustStrategy.TRUST_ALL_CERTS)
                        .withProxyUrl(SERVER_ADDRESS:PORT)                 
                        .withProxyUsername(USERNAME)
                        .withProxyPassword(PASSWORD)
                        .build())
                    .build())
            .withFactories(CalendarModel.FACTORIES)
            .build();
```

## 2020-07-21 Added Feed cancellation Endpoint
Allegro can now delete a feed and its feeds related entries from the ObjectStore. To do you you need to pass
+ The IFeedId object

For example:

```java
    	    allegroApi_.deleteFeed(new FeedId.Builder()
    	        .withId(feed.getId())
    	        .build());
```

## 2020-06-22 Added Certificate Authentication
Allegro can now authenticate using client certificates. To do you you need to pass
+ The name of a .p12 file containing the client certificate and private key
+ The password for the certificate file
+ The session auth URL for your pod, for a pod with the url https://yourname.symphony.com this is typically 
https://yourname-api.symphony.com or https://yourname.symphony.com:8444 or https://yourname-api.symphony.com:8444
+ The key manager auth URL for your pod.

For example:

```java
allegroApi_ = new AllegroApi.Builder()
      .withPodUrl(podUrl_)
      .withObjectStoreUrl(objectStoreUrl_)
      .withCertFilePath(certFile_)
      .withCertFilePassword(certPassword_)
      .withSessionAuthUrl(sessionAuthUrl_)
      .withKeyAuthUrl(keyAuthUrl_)
      .build();
```


## 2020-06-03 Transactional Write
A method was added to write multiple objects in an atomic transaction:

```java
  /**
   * Store the given collection of objects in a single atomic transaction.
   * 
   * The underlying storage infrastructure has a limit on the number of items which can be stored in a single
   * transaction (currently 25). Each object store object requires 4 rows in the underlying database so the
   * current limit on the number of objects which can be successfully written in a single transaction is 
   * currently 25 / 4 = 6. If too many objects are passed the request will fail throwing a BadRequestException
   * and no change will have been made to the object store.
   * 
   * @param objects Objects to be stored.
   * 
   * @throws BadRequestException If the transaction is too large.
   */
  void storeTransaction(Collection<IAbstractStoredApplicationObject> objects);
```

## 2020-05-24 fetchUserById and fetchStreams
Methods were added to fetch user info by numeric userId and stream membership for the calling user:

```java
  /**
   * Fetch information about streams (conversations or threads) the caller is a member of.
   * 
   * @param fetchStreamsRequest Request parameters.
   * 
   * @return A list of objects describing streams of which the caller is a member.
   */
  List<IStreamAttributes> fetchStreams(FetchStreamsRequest fetchStreamsRequest);

  /**
   * Fetch information about a user given an external user ID.
   * 
   * @param userId  The external user ID of the required user.
   * 
   * @return The user object for the required user.
   * 
   * @throws NotFoundException If the given userName cannot be found.
   */
  IUserV2 fetchUserById(PodAndUserId userId) throws NotFoundException;
```
 
## 2020-05-07 Partition Methods
Methods were added to fetch a Partition object (i.e. the object describing a partition including it's ID object), fetch
an object based on it's partition and sort key, and to return the ModelRegistry used by Allegro.
 
```java
  /**
   * Fetch a Partition object.
   * 
   * @param query The query parameters for the Partition required.
   * 
   * @return The Partition object which describes the partition.
   */
  IPartition fetchPartition(PartitionQuery query);

  /**
   * Return the ModelRegistry used by Allegro.
   * 
   * @return the ModelRegistry used by Allegro.
   */
  ModelRegistry getModelRegistry();

  /**
   * Fetch an object by its Partition and Sort key.
   * 
   * @param partitionHash The Partition of which the object is a member.
   * @param sortKey       The object's sort key.
   * 
   * @return The required object.
   * 
   * @throws PermissionDeniedException If the caller does not have access to the required object.
   * @throws NotFoundException         If the requested object does not exist.
   */
  IStoredApplicationObject fetchObject(Hash partitionHash, String sortKey);
  ```

## 2020-04-15 Caller provided session and keymanager tokens
It is now possible to provide session and keymanager tokens rather than login credentials. This snippet passes in both session tokens
and login credentials to the API constructor. If **sessionToken** and **keymanagerToken** are both non-null then they are used,
otherwise if **serviceAccount** and **credentialFile** are both present they are used to log in and the session and keymanager
tokens are printed to stdout.

**The session and keymanager tokens are bearer authentication credentials and should be protected from disclosure.**

```java
allegroApi_ = new AllegroApi.Builder()
        .withPodUrl(podUrl_)
        .withObjectStoreUrl(objectStoreUrl_)
        .withUserName(serviceAccount_)
        .withRsaPemCredentialFile(credentialFile_)
        .withFactories(CalendarModel.FACTORIES)
        .withTrustAllSslCerts()
        .withTraceFactory(traceFactory_)
        .withSessionToken(sessionToken_)
        .withKeymanagerToken(keymanagerToken_)
        .build();
      
    userId_         = allegroApi_.getUserId();
    
    log_.info("PodId is " + allegroApi_.getPodId());
    log_.info("UserId is " + userId_);

    if(credentialFile_ != null)
    {
      System.out.println("SessionToken " + allegroApi_.getSessionToken());
      System.out.println("KeyManagerToken " + allegroApi_.getKeyManagerToken());
    }
```

## 2020-04-15 Public Release 0.2.0
Version 0.2.0 was released to Maven Central including all changes below.

## 2020-04-15 Cryptolib upgrade to 1.57.2
The embedded copy of cryptolib was upgraded to 1.57.2 which includes a fix for a failure to
load native libraries under JDK 8u242.

## 2020-04-14 Support for entitlements and JWT session token generation and verification
Methods added to access create and fetch operations for entitlements and entitlement mappings as
well as methods to obtain JWT generator and verifier instances and an entitlement validator.

## 2020-04-03 Release 0.1.19
Version 0.1.19 was released internally including all changes below.

## 2020-04-07 Fixes for Pods with different internal and external PodIds
Several bugs which impact pods with a different internal and external pod ID were fixed.

## 2020-04-04 Addded methods to fetch users by their login name
You can now find users in your local pod by their login name.

```java

  /**
   * Fetch information about a user given a user (login) name.
   * 
   * @param userName  The userName with which the required user logs in.
   * 
   * @return The user object for the required user.
   * 
   * @throws NotFoundException If the given userName cannot be found.
   */
  IUserV2 getUserByName(String userName) throws NotFoundException;

  /**
   * Fetch information about one or more users given a user (login) name.
   * 
   * @param userNames  The userName with which the required users log in.
   * 
   * @return A list of responses and errors.
   */
  IV2UserList getUsersByName(String... userNames);
```

## 2020-04-03 Added type specific decryptObject() method
You can now get a typed reference to a decrypted ApplicationObjectPayload by calling the method

```java
  /**
   * Deserialize and decrypt the given object.
   * 
   * @param <T> Type of the required object payload.
   * 
   * @param storedApplicationObject An encrypted object.
   * @param type                    Type of the required object payload.
   * 
   * @return The decrypted object.
   * 
   * @throws IllegalStateException If the decrypted payload is not an instance of the required type.
   */
  public <T extends IApplicationObjectPayload> T decryptObject(IStoredApplicationObject storedApplicationObject, Class<T> type);
```

## 2020-04-03 Release 0.1.18
Version 0.1.18 was released internally including all changes below.

## 2020-03-17 Remove mutable attribute from AbstractApplicationObjectPayload
In order to allow an update to be made from an ApplicationObjectPayload, the class AbstractApplicationObjectPayload
(which is the superclass of all application (encrypted) payloads and (unencrypted) header objects) provided a
**getStoredApplicationObject()** method. Unfortunately this was implemented as a getter on a mutable attribute in
the AbstractApplicationObjectPayload which is problematic for a number of reasons.

TL;DR the mutable attribute has been removed, with the effect that when you create an object, the application
payload *can not* hold a reference to the StoredApplicationObject because the payload has to be created first.

What does this mean? The method IAllegroApi.newApplicationObjectUpdater(existing) now takes an IStoredApplicationObject
instead of an IAbstractApplicationPayload. 

When an application object payload is returned from fetch operations you can just call getStoredApplicationObject() on the payload
to call IAllegroApi.newApplicationObjectUpdater(existing). In cases where you create the payload and then update it
you need to retain a separate reference to the StoredApplicationObject which is returned by the
**store(IAbstractStoredApplicationObject object)** method.

## 2020-03-12 Allow for filtering of feed subscriptions by sort key prefix
When upserting a partition it is now possible to filter the records which will be delivered to the feed by sort key prefix
as in this example

```java
builder = new UpsertFeedRequest.Builder()
    .withName(FILTER_FEED_NAME)
    .withPermissions(permissions)
    .withPartitionSelection(
        new PartitionId.Builder()
        .withName(PARTITION_NAME)
        .build(), FILTER_PREFIX
        )
    ;

filterFeed_ = allegroApi_.upsertFeed(builder.build());
```
The withPartitionSelection method on UpsertFeedRequest.Builder takes a partition ID and a prefix. Only objects whose sort key begins with
the given prefix will be forwarded to the feed. If the sort key prefix is null then all records will be forwarded.

## 2020-03-05 Separation of IMultiTenantAllegroApi !!!BREAKING CHANGE!!!
It is now possible to authenticate to the object store but not a pod to access only multi-tenant features
of the API. There is an example of this in 
[CreateToDoItemInTwoStages.java](https://github.com/SymphonyOSF/oss-allegro-examples/blob/master/calendar/src/main/java/com/symphony/s2/allegro/examples/calendar/CreateToDoItemInTwoStages.java#L174)

As part of this refactoring a number of classes have been moved from sub-packages into the main API package
**com.symphony.oss.allegro.api**

## 2020-02-04 Deletion now takes IStoredApplicationObject !!!BREAKING CHANGE!!!
The signature of the delete method is now IAllegroApi.delete(IStoredApplicationObject item, DeletionType deletionType);

Previously this method took an IApplicationObjectPayload, call the getStoredApplicationObject() method on that object
to call the new method.

## 2020-01-31 Added BigDecimal as a type to core model
BigDecimal values can now be added to models by reference to a typedef in the Core model.
An example of this can be seen in the Calendar example model [calendar.json](https://github.com/SymphonyOSF/oss-allegro-examples/blob/master/calendar/src/main/canon/calendar.json#L35)
and the example program [CreateToDoItemInTwoStages.java](https://github.com/SymphonyOSF/oss-allegro-examples/blob/master/calendar/src/main/java/com/symphony/s2/allegro/examples/calendar/CreateToDoItemInTwoStages.java#L119)

## 2020-01-31 Encrypted Payload Can Now be Created Separately
It is now possible to create a stand alone **IEncryptedApplicationPayloadAndHeader** or **IEncryptedApplicationPayload**
and to create an **IStoredApplicationObject** separately containing the already encrypted payload.

The example program [CreateToDoItemInTwoStages.java](https://github.com/SymphonyOSF/oss-allegro-examples/blob/master/calendar/src/main/java/com/symphony/s2/allegro/examples/calendar/CreateToDoItemInTwoStages.java) illustrates this.

## 2020-01-31 ApplicationObjectBuilders Return interfaces
ApplicationObjectBuilder.build() and ApplicationObjectUpdater.build() now return IStoredApplicationObject instead of StoredApplicationObject.

ApplicationObjectDeleter.build() now returns IDeletedApplicationObject instead of DeletedApplicationObject.

## 2020-01-29 Paged Partition Queries Return Rows in Order
Paged Partition queries now always return retrieved rows in ascending order of sort key, even when calling
<code>IObjectPage.fetchPrevPage()</code>.

## 2020-01-28 Release 0.1.9
Release 0.1.9 was made including all of the changes below.

## 2020-01-28 Paginated Partition Queries
A new method to make paginated queries from a partition has been added which is intended for use by a UI which wished
to implement a paged view of the data in a partition.
A paginated query takes a single Query (allows for some number of objects to be retrieved from a single Partition):

```java
IObjectPage page = allegroApi_.fetchPartitionObjectPage(new PartitionQuery.Builder()
      .withName(PARTITION_NAME)
      .withMaxItems(FETCH_PARTITION_PAGE_SIZE)
      .build()
      );
```

The resulting page object allows you to to retrieve the data rows in the page and to fetch the next and previous pages:

```java
for(IStoredApplicationObject item : page.getData())
{
  IApplicationObjectPayload payload = allegroApi_.open(item);
  
  System.out.println(item.getAbsoluteHash() + " " + item.getCanonType() + " " + payload.getCanonType());
}

page = page.fetchNextPage();
```


## 2020-01-24 Release 0.1.7
Release 0.1.7 was made including all of the changes below.


## 2020-01-24 Initial Implementation of Datafeed 2.0 Client
Methods have been added to access DataFeed 2.0, this is an initial implementation there is more work to be done.

```java
// List all available feeds for the calling user
List<FeedId> feedIds = allegroApi_.listMessageFeeds();

// Create a feed
FeedId feedId = allegroApi_.createMessageFeed();

// Read a feed - only single threaded (synchronous) calls are implemented at this time.
AckId ackId = null;

while(true)
{
  ackId = allegroApi_.fetchFeedMessages(new FetchFeedMessagesRequest.Builder()
      .withFeedId(feedId)
      .withAckId(ackId)
      .withConsumerManager(new ConsumerManager.Builder()
        .withConsumer(IReceivedChatMessage.class, (item, trace) ->
        {
          System.out.println(item.getPresentationML());
        })
        .build()
      )
      .build()
      );
}
```

At this time we cannot detect the expiry of datafeed credentials so they cannot be autor renewed and a feed will probably fail after
10 minutes or so.

## 2020-01-20 Async Processing !!!BREAKING CHANGE!!!
The way in which asyncronous processing of results has changed.

The method 

```java
public IFugueLifecycleComponent subscribeToFeed(SubscribeFeedObjectsRequest request);
```

has been removed and the class **ThreadSafeConsumerManager** has been renamed **AsyncConsumerManager**.

All parameters relating to thread pool sizes have been moved to **AsyncConsumerManager**.
The same methods can now be called for synchronous and asynchronous processing, the type of ConsumerManager provided
now determines which type of handling is performed.

All fetch requests now support multiple queries in a single call, for synchronous requests this is
equivalent to making a separate request for each query, but for asynchronous requests the queries are
all executed in parallel.

For synchronous methods the call to **.withMaxItems(int max)** has moved to the query, so this code,
which works on AllegroAPI 0.2.15:

```java
allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
  .withName(PARTITION_NAME)
  .withMaxItems(10)
  .withConsumerManager(new ConsumerManager.Builder()
      .withConsumer(IToDoItem.class, (item, trace) ->
      {
        System.out.println("Payload: " + item);
      })
      .build()
      )
  .build()
  );
```

needs to be refactored like this for versions 0.2.16 onwards:


```java
allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
  .withQuery(new PartitionQuery.Builder()
      .withName(PARTITION_NAME)
      .withMaxItems(10)
      .build()
      )
  .withConsumerManager(new ConsumerManager.Builder()
      .withMaxItems(10)
      .withConsumer(IToDoItem.class, (item, trace) ->
      {
        System.out.println("Payload: " + item);
      })
      .build()
      )
  .build()
  );
```

For asynchronous calls the call to **.withSubscriberThreadPoolSize(int size)** abd **.withSubscriberThreadPoolSize(int size)**
have moved to the AsyncConsumerManager, so this code,
which works on AllegroAPI 0.2.15:

```java
IFugueLifecycleComponent subscriber = allegroApi_.subscribeToFeed(new SubscribeFeedObjectsRequest.Builder()
    .withName("myCalendarFeed")
    .withOwner(ownerUserId)
    .withSubscriberThreadPoolSize(10)
    .withHandlerThreadPoolSize(90)
    .withConsumerManager(new ThreadSafeConsumerManager.Builder()
      .withConsumer(IToDoItem.class, (message, traceContext) ->
      {
        log_.info(message.toString());
      })
      .withUnprocessableMessageConsumer((item, trace, message, cause) ->
      {
        log_.error("Failed to consume message: " + message + "\nPayload:" + item, cause);
      })
      .build()
    )
  .build()
);
```

needs to be refactored like this for versions 0.2.16 onwards:

```java
IAllegroQueryManager queryManager = allegroApi_.fetchFeedObjects(new FetchFeedObjectsRequest.Builder()
  .withQuery(new PartitionQuery.Builder()
      .withName("myCalendarFeed")
      .withOwner(ownerUserId)
      .build()
      )
    .withName("myCalendarFeed")
    .withOwner(ownerUserId)
    .withConsumerManager(new AsyncConsumerManager.Builder()
        .withSubscriberThreadPoolSize(10)
        .withHandlerThreadPoolSize(90)
        .withConsumer(IToDoItem.class, (message, traceContext) ->
        {
          log_.info(message.toString());
        })
        .withUnprocessableMessageConsumer((item, trace, message, cause) ->
        {
          log_.error("Failed to consume message: " + message + "\nPayload:" + item, cause);
        })
        .build()
    )
  .build()
);
```

The return value for asynchronous requests has changed from **IFugueLifecycleComponent** to
**IAllegroQueryManager** and this interface provides the methods
**boolean isIdle()** which indicates if the request is idle and
**void waitUntilIdle() throws InterruptedException** which blocks until the request becomes idle.

In this context, **idle** means that there is no data available from the server.
In the case of a database query, being idle indicates that there is no more data to consume, therefore
in these cases this method never returns false once it has returned true.
In the case of a feed query, being idle indicates that there is no more data available for the moment,
but more data could become available at any time. 

These methods are most likely to be useful in test scenarios.

## 2020-01-20 Added fetchObjectVersions
This method returns all versions of a base object.
For example code, see [ListItemVersions.java](https://github.com/SymphonyOSF/oss-allegro-examples/blob/master/calendar/src/main/java/com/symphony/s2/allegro/examples/calendar/ListItemVersions.java#L111)
