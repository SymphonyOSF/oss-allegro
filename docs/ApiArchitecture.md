---
nav_order: 20
---
# The Architecture of the API

## Immutable Objects and the Builder Pattern
The Allegro API makes extensive use of immutable objects and the builder pattern. For example, the following code snippet 
shows how to construct an instance of the main API class:

```java
allegroApi_ = new AllegroApi.Builder()
  .withPodUrl(podUrl)
  .withUserName(userName)
  .withRsaPemCredentialFile(credentialFile)
  .build();
```

The class **AllegroApi** cannot be instantiated directly, to create an instance you instantiate an **AllegroApi.Builder**, call
several fluent setter methods, and then call the **build()** method, which returns an instance of **IAllegroApi**. In all cases
the object returned from the **build()** method of a **Builder** is immutable in the sense that parameters set in the builder
cannot later be changed in the instantiated object.

It is a general characteristic of the API that for any class **Foo** there is an interface **IFoo**. The use of interfaces
makes for a cleaner API and avoids exposing unnecessary implementation details to the caller, and is necessary
for some patterns including the hierarchical builder pattern described below.

Most methods in the API take a single request parameter which is also created via a Builder, like this:

```java
IFeed feed = allegroApi_.upsertFeed(new UpsertFeedRequest.Builder()
    .withName("myCalendarFeed")
    .withPermissions(permissions)
    .withPartitionIds(
        new PartitionId.Builder()
        .withName(ToDoItem.TYPE_ID)
        .withOwner(ownerId_)
        .build()
        )
    .build());
```

In this example the **UpsertFeedRequest** takes a structured **PartitionId** object which is itself constructed by a builder.
The fluent nature of the setters (named **withXXX()**) means that the code to call these methods can still be written in
a concise way.

Any constraints on the parameters of a builder are checked in the **build()** method, which may throw an **IllegalStateException**,
individual fluent setter methods may throw **IllegalArgumentException** although to the maximum extent possible the API is
type safe to avoid the potential to pass values which are outright invalid.

In some cases overloaded implementations of fluent setters may be provided as a convenience, for example **AllegroApi.Builder** 
provides **withPodUrl(String podUrl)** as well as **withPodUrl(URL podUrl)**, the implementation with the String parameter
will throw **IllegalArgumentException** if the passed value is not a valid URL.

Builder instances can be re-used and the **build()** method should have no side effects on the builder itself, while it may
be convenient for the implementation of the **build()** method to set builder member variables (particularly where defaults or
convenience methods are provided) to so would be confusing to the caller and is therefore to be avoided.

## Canon Object Schemas
The Allegro API uses Canon Schemas (see [https://github.com/symphonyoss/canon](https://github.com/symphonyoss/canon)).
It is not required, but recommended that Allegro applications define Canon schemas for their own objects as
this allows the API to do more for you, such as doing type sensitive routing of objects to multiple consumers
when fetching objects.

# Implementation Details

## Immutable Objects and the Builder Pattern
Allegro builder implementations are sub-classed from a base builder class which allows for inheritance among the various
request objects. For example **NamedUserIdObjectRequest** is a base request class for requests referring to a 
**NamedUserIdObject**, a (very) partial copy of the implementation is shown below: 

```java
public class NamedUserIdObjectRequest
{
  NamedUserIdObjectRequest(AbstractBuilder<?,?> builder)
  {
    id_   = builder.id_;
    name_ = builder.name_;
    owner_ = builder.owner_;
  }
  
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends NamedUserIdObjectRequest> extends BaseAbstractBuilder<T,B>
  {
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    public T withName(String name)
    {
      name_ = name;
      
      return self();
    }
    
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);

      faultAccumulator.checkValueCount("ID and Name", 1, 1, id_, name_);
      faultAccumulator.checkValueCount("ID and Owner", 0, 1, id_, owner_);
    }
  }
}
```

Note that the constructor is package private, the class cannot be directly instantiated from application code, and it takes an instance
of **AbstractBuilder**. This builder, as the name suggests, is abstract, parameterised for the concrete type of the builder (**T**) as well
as the built type (**B**), and extends **BaseAbstractBuilder<T,B>**.

This **AbstractBuilder** contains the fluent setters required to set immutable parameters of the built type (only one of which is shown),
note that the **self()** method (defined by **BaseAbstractBuilder<T,B>**) is used to return a type safe instance of the
concrete builder type.

The **AbstractBuilder** also contains a **validate(FaultAccumulator faultAccumulator)** method
which will be called when the **build()** method is invoked. The implementation
of this method calls the super implementation to ensure that all levels of the class hierarchy are validated.

There is no concrete **Builder** class here so **NamedUserIdObjectRequest** cannot be instantiated directly.

A sub-class of this, **NamedUserIdObjectOrHashRequest**, represents a request which may contain a **NamedUserIdObject** _or_
the caller may provide a **Hash** instead. This is used for requests relating to an existing Partition where it may
be convenient for the caller to provide the ID details or they may prefer to pass the ID Hash (which can be computed from the
ID object) directly.

```java
public class NamedUserIdObjectOrHashRequest extends NamedUserIdObjectRequest
{
  NamedUserIdObjectOrHashRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    hash_ = builder.hash_;
  }
  
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends NamedUserIdObjectOrHashRequest> extends NamedUserIdObjectRequest.AbstractBuilder<T,B>
  {
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    public T withHash(Hash hash)
    {
      hash_ = hash;
      
      return self();
    }
    
    protected void validate(FaultAccumulator faultAccumulator)
    {
      faultAccumulator.checkValueCount("Hash, ID and Name", 1, 1, hash_, id_, name_);
      faultAccumulator.checkValueCount("Hash, ID and Owner", 0, 1, hash_, id_, owner_);
    }
  }
}
```
Notice that **NamedUserIdObjectOrHashRequest** extends **NamedUserIdObjectRequest** and
**NamedUserIdObjectOrHashRequest.AbstractBuilder** extends **NamedUserIdObjectRequest.AbstractBuilder**.

The implementation of this class follows the same pattern, adding a **Hash** to the attributes and performing appropriate 
validation logic. Normally the validate logic should call super.validate, but in this case it is overridden because the hash
may be provided _instead_ of the attributes provided by the super class.

Again, there is no concrete builder so this class cannot be instantiated, however the sub-class **FetchPartitionObjectsRequest**
can:

```java
public class FetchPartitionObjectsRequest extends NamedUserIdObjectOrHashRequest
{
  FetchPartitionObjectsRequest(AbstractBuilder<?,?> builder)
  {
    super(builder);

    scanForwards_     = builder.scanForwards_;
    maxItems_         = builder.maxItems_;
    after_            = builder.after_;
    sortKeyPrefix_    = builder.sortKeyPrefix_;
    consumerManager_  = builder.consumerManager_;
  }
  
  public static class Builder extends AbstractBuilder<Builder, FetchPartitionObjectsRequest>
  {
    public Builder()
    {
      super(Builder.class);
    }

    protected FetchPartitionObjectsRequest construct()
    {
      return new FetchPartitionObjectsRequest(this);
    }
  }

  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends FetchPartitionObjectsRequest> extends NamedUserIdObjectOrHashRequest.AbstractBuilder<T,B>
  {
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }

    public T withScanForwards(boolean scanForwards)
    {
      scanForwards_ = scanForwards;
      
      return self();
    }
    
    protected void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      if(maxItems_ != null && maxItems_ < 1)
        faultAccumulator.error("maxItems must be at least 1, or not set.");
    }
  }
}
```

This class also has an **AbstractBuilder** which means that it too could be sub-classed, but it also defines a concrete
**Builder** class. The concrete builder provides a default (zero argument) constructor, and a **construct()** method which
returns an instance of the concrete built type (**FetchPartitionObjectsRequest** in this case).

The actual **build()** method is implemented by **BaseAbstractBuilder<T,B>** which ensures that the **validate** method
is called at the appropriate time.
