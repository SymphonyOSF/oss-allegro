---
nav_order: 200
has_children: true
permalink: /docs/Partitions
---
# Partitions
A Partition is a dataset within the Object Store. Every object belongs to exactly one Partition.

Partitions are identified by a hash, this hash is known as the object's Partition Key, and a separate
Sort Key determines the object's position within the Partition.

The hash identifier for a Partition is generated from an ID object in Canonical JSON form. This ID object
includes the user ID of the creator and a name, if the user ID is not specified then it is implicitly the
ID of the calling user. Other attributes may also be added to the object if needed by the application.
The Object Store ensures that all Partition ID objects include the ID of the calling user when a Partition
is created, which ensures that a malicious caller cannot create Partitions with a hash ID which collides with
one for another user.