# oss-allegro
Allegro API for Symphony

For documentation, see [https://allegro.oss.symphony.com/](https://allegro.oss.symphony.com/)

For JavaDocs, see [https://javadoc.io/doc/com.symphony.oss.allegro/allegro-api/latest/index.html](https://javadoc.io/doc/com.symphony.oss.allegro/allegro-api/latest/index.html)

# Change Log

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
