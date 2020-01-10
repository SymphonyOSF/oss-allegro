---
nav_order: 10
---
# The Symphony 2.0 Object Store
The Symphony Object Store is an encrypted key/value store which leverages the Symphony Key Manager and provides
a service to store arbitrary content. Objects stored via the Allegro API are immutable, encrypted and can be 
identified by the hash of their contents. A [Cryptographic Hash Function](https://en.wikipedia.org/wiki/Cryptographic_hash_function) is a mathematical algorithm that maps data of arbitrary size (often called the "message") to a bit string of a fixed size
(the "hash value", "hash", or "message digest").

In order to represent real world objects where the ability to update the contents of an object may be required,
the Object Store supports the idea of object versions. When an object is updated a new version of the object is
created, both the original and updated versions of the object continue to exist, and can be accessed via their
hash, but the hash of the original version of the object is considered to be the baseHash of the whole series
of versions, and the hash of each version is considered to be the absoluteHash of that version.

The Object Store provides an efficient mechanism to fetch the latest (current) version of an object via
the baseHash, as well as the ability to enumerate all of the versions. Of course a specific version of an
object can always be retrieved by its absoluteHash too.

The Object Store organizes objects into datasets called Partitions. Partitions are identified by a set of attributes
including a name and an owner (Symphony user ID). The ID of a Partition is the hash of an ID object constructed
from these attributes in a strictly defined way.

Each user can only create Partitions which include their own userId which
prevents denial of service attacks by users creating a Partition with a name calculated to collide with that used
by another user. The owner of a Partition can assign entitlements to the Partition to control who has access to
the data contained in a partition.

In addition to these physical access controls, in order to read the contents of an object, a user needs access to
the encryption key which is controlled by the usual conversation membership functions.

If all of the above constraints are met, it is possible for multiple users, potentially from different pods, to access
objects in the same Partition. This forms a platform for the implementation of many kinds of application where data
access is controlled by the Symphony security model.

A Partition is an ordered
collection of objects, each object has a Sort Key which determines its order within the Partition. The creator of the
object determines the object's Sort Key and this allows for data to be organized in a way which supports the
functional requirements of the application. The creation date of the object is one obvious potential Sort Key,
and for many applications this is appropriate, but any other value can also be used. When objects are updated their
Sort Key can be changed if required.

If two objects in the same Partition are created with the same Sort Key then there is no "correct" order for those 
objects in any absolute sense, but in order to ensure that all observers see the same data in the same order
the absolute hash of the object is appended to the user provided Sort Key.

All updates to the Object Store are transactional and protected by optimistic locking, if the object has
already changed from the expected version then the update fails.

##Update Notifications

The Object Store provides a feed mechanism which allows a caller to be notified when an object is created or updated 
in one or more partitions.
A Feed is a persistent queue of update notifications onto which a copy of all newly created or updated objects is
pushed.
When messages are consumed from this queue they are actively acknowledged by the Allegro API client so that the
system provides an at least once delivery guarantee.

In general update notifications will be delivered in the order in which they occurred, but this is not guaranteed, and
in order to achieved the desired levels of performance and scalability, most applications will need to implement on
a multi-threaded basis. For these reasons, consuming applications should be designed on the basis of an eventually
consistent model, and if the order of processing is significant then timestamps or sequence numbers need to be
included within the stored objects so that the correct ordering can be asserted.

