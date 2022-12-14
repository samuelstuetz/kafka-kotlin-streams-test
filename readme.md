# Spring Boot App - Accounts and Events

This example application was written as challenge.
Kafka can be used to create an event sourcing database with all the crud features needed.
This min project was testing such an approach which is why there is no usual database used just Kafka.

The initial challenge for this project was to store some data and then implement some event streaming logic. This could be done in Postgres alone or Postgres with some event servive. I use just only the event storage for all of it.

## Basic setup
- written a year ago might need some updates
- used free confluent cloud kafka for backend setup (install ccloud confluent cli see [purge.sh](./purge.sh))
- springboot 5
- kafka plugins
- basic http endpoints
  - coroutines everywhere
- junit5 and mockkk for testing

## Serializer disclaimer
**TLDR** I didn't want to debug my serializer spring setup so I just avoided the problem by not typing input outputs, normally I like strong typing and would fix that in any more serious project.
In any serious production use case Kafka Avro + registry is better than json most times.

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
