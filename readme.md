# Spring Boot App - Accounts and Events

This example application was written as challenge. 
I have use Kafka with Apache Spark and a long time ago for typical event logging.  
The claim that an entire event source database can be built on Kafka I wanted to test how that works,
which is why Kafka was used here without any extra databse.

The requirements for accounts (100-1000) and events (1000-10000) a day did not require Kafka.
Handling accounts in postgres and then using any event streaming solution for the event store would have worked.

*Disclaimer: I used Micronaut, Kotlin and Kotest in recent history.
I assumed Spring Boot and Kafka would be able to handle Json serialization consistently.
So I hacked a poor mans String Serialization because I did not want to deal with fixing Spring to my usecase.
When I worked previously (over 6 years ago) with Kafka I used protobuf custom and then avro. 
In any serious production use case Kafka and Json are wrong and Avro + registry should be used (out of scope today).*

I did not fix all tests or implement all features yet. I will clean this up and put it on github.

## build and deploy

### scripts
**just read the make file**
- `make run` to startup without id, otherwise use that no envs needed
- `make clean build test` to build it in ci no need to know gradle, java or anything, java 11+ should be around.
- `purge.sh` won't work without credentials just to document needed commands, I provided the api keys they will work for running the app a few weeks.
- `make test-it` simple bash script to do a manual test run

### settings
- kafka ccloud configured in `application.properties` (will be deactivated in a few weeks),
  alternative install kafka helm (see Makefile) or some other way
- **topics** and **store names** found in `models.kt:Topics` and `models.kt:NamedStores` enums.
  As soon as they are in the yaml you loose all IDE references and help.
  Editing the code is as fast as the yaml. In production this should move into configmaps.
- **application-id** if changed will create new changelog topics for the stores.

## topology
 - account topic receives all account create/delete/update events 
 - account_store materializes the events to a consistent table `KafkaStreamTopology.dailyStatistics`
 - event topic receives all account associated events
 - daily_stats_windowed is a minimal implementation of aggregate statistics `KafkaStreamTopology.dailyStatistics`

### ttl notes 
account topic is compacted. Each key keeps the latest info.
KTable changelogs should never timeout 
data should be deleted via tombstone events (null value) 
this does work (in test utils) but not in bash test.

event topic has a ttl of 30 days, meaning the kafka cleanup should forget data older than that.
statistics are materialized in the store and its changelog and should never disappear.
I did not test that part yet,

## endpoints
- accounts 
  - list
  - create
  - update
  - delete
- events: TTL 30 days
  - list
  - publish
  - statistics

## folder structure

- configs
  - KafkaConfig: topics and basic configurations
  - KafkaStreamsTopology: the streams builders aka the stores (KTable)
- controllers: endpoints only
- repositories
  - EventStore: the main thing for events statistics
  - AccountStore: the account db repo more or less
  - ... other is just helper code
  - ConsumerForKey is a helper for getting the list events working, not a solution that can be considered viable, I assumed Kafka could stream by key. 
- exceptions.kt: just a basic exception, all exceptions should be here so anybody can easily see what can fail in this app 
- models.kt: compact file with all the data types. These are not classes just types. All functions should be extension functions elsewhere.
- utils.kt: irrelevant stuff just to keep the relevant code elsewhere clean and readable
