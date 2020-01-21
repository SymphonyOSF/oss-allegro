# oss-allegro
Allegro API for Symphony

For documentation, see [https://allegro.oss.symphony.com/](https://allegro.oss.symphony.com/)

# Change Log

## 2020-01-20 Async Processing !!!BREAKING CHANGE!!!
The way in which asyncronous processing of results has changed.

The method 

```java
public IFugueLifecycleComponent subscribeToFeed(SubscribeFeedObjectsRequest request);
```

has been removed and the class **ThreadSafeConsumerManager** has been renamed **AsyncConsumerManager**.

All parameters relating to thread pool sizes or row count limits have been moved to **ConsumerManager** or **AsyncConsumerManager**.
The same methods can now be called for synchronous and asynchronous processing, the type of ConsumerManager provided
now determines which type of handling is performed.

For synchronous methods the call to **.withMaxItems(int max)** has moved to the ConsumerManager, so this code,
which works on AllegroAPI 0.2.15:

```java
allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
  .withName(ToDoItem.TYPE_ID)
  .withOwner(allegroApi_.getUserId())
  .withMaxItems(10)
  .withConsumerManager(new ConsumerManager.Builder()
      .withConsumer(IToDoItem.class, (item, trace) ->
      {
        System.out.println("Header:  " + item.getStoredApplicationObject().getHeader());
        System.out.println("Payload: " + item);
        
        update(item);
      })
      .build()
      )
  .build()
  );
```

needs to be refactored like this for versions 0.2.16 onwards:


```java
allegroApi_.fetchPartitionObjects(new FetchPartitionObjectsRequest.Builder()
  .withName(ToDoItem.TYPE_ID)
  .withOwner(allegroApi_.getUserId())
  .withConsumerManager(new ConsumerManager.Builder()
      .withMaxItems(10)
      .withConsumer(IToDoItem.class, (item, trace) ->
      {
        System.out.println("Header:  " + item.getStoredApplicationObject().getHeader());
        System.out.println("Payload: " + item);
        
        update(item);
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
IFugueLifecycleComponent subscriber = allegroApi_.fetchFeedObjects(new FetchFeedObjectsRequest.Builder()
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

## 2020-01-20 Added allegro-test Module
ApiIntegrationTest is intended to be a test suite which exercises every aspect of the API although it does
not yet have complete coverage.

It can be executed against a local in-memory server, a locally running AWS server or a deployed server.

It is intended to be a clean sheet repeatable integration test but the API does not yet support some of the delete methods
needed to make this possible.

The **--REPEAT true** flag can be passed to run the test a second time.

## 2020-01-20 Added fetchObjectVersions
This method returns all versions of a base object.
For example code, see [ListItemVersions.java](https://github.com/SymphonyOSF/oss-allegro-examples/blob/master/calendar/src/main/java/com/symphony/s2/allegro/examples/calendar/ListItemVersions.java#L111)
