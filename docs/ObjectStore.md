---
nav_order: 10
title: Home
---
# The Symphony 2.0 Object Store
The Symphony Object Store is an encrypted key/value store which leverages the Symphony Key Manager and provides
a service to store both chat messages and other content. Objects stored via the Allegro API are immutable, encrypted,
signed and identified by the hash of their contents. A [Cryptographic Hash Function](https://en.wikipedia.org/wiki/Cryptographic_hash_function) is a mathematical algorithm that maps data of arbitrary size (often called the "message") to a bit string of a fixed size (the "hash value", "hash", or "message digest").

In order to represent real world objects where the ability to update the contents of an object may be required,
the Object Store supports the idea of object versions. When an object is updated a new version of the object is
created, both the original and updated versions of the object continue to exist, and can be accessed via their
hash, but the hash of the original version of the object is considered to be the baseHash of the whole series
of versions, and the hash of each version is considered to be the absoluteHash of that version.

The Object Store provides an efficient mechanism to fetch the latest (current) version of an object via
the baseHash, as well as the ability to enumerate all of the versions. Of course a specific version of an
object can always be retrieved by its absoluteHash too.

The Object Store also supports Sequences of objects, which may be current or absolute. A Sequence is an ordered
collection of objects, and an object may be a member of one or more Sequences. All objects created via Allegro
will always be a member of at least one sequence, because there is a sequence of "All objects in this Pod".
This sequence can be thought of as the Pod's firehose.

Sequences can also represent the set of messages delivered to a particular user (that user's datafeed) or the
set of messages in a conversation (a thread).

All objects in the Object Store have a createdDate, and objects in a sequence are ordered by their createdDate
concatenated with their hash. This ensures that objects with identical values for createdDate appear in the same
order to all observers. The timestamps in the Object Store can accept up to nanosecond resolution, although most
computer clocks are less precise (not to mention accurate) than this, and in any event, regardless of the 
precision of the timestamp it will always be possible for two objects to be created at the same tick of the clock.
If and when this happens there is no "correct" order for those objects in any absolute sense, but it is much
better if everyone agrees on what the ordering is, and the use of the hash of objects as a "tie breaker" in
such situations is a way of achieving this.

The difference between current and absolute sequences is in how object versions are dealt with. If an object is
updated, then the new version is appended to an absolute sequence with the createdDate of the new version. This means
that every version of an object appears on the sequence at the time it was created, which is good for use cases
such as an audit trail where you want to see everything which happened.

When an updated version of an object is added to a current sequence, it is added at the time of the original
version of the object, replacing the previous version. This means that if you enumerate the members of a
current sequence, each versioned object will appear only once, and it will be the latest or current version of
the object which appears. This is good for use cases where you need a database table. 
