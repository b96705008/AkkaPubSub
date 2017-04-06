# AkkaPubSub

## Akka
* FSM - status management
* http - akka http client for JSON response 
* Stream - combine with spark and actor

## Kafka
* Actor - combine with Akka actor with Kafka
* Stream - combine with spark streaming

## Redis
* PubSub - replace Kafka with Redis as lightweight PubSub engine

## Mongo
* Dump spark data to MongoDB using Mongo-Hadoop or Mongo-Spark
* Test the auth connection
* MongoDB version should greater or equal to 3.2
* If using auth, the db user should be assigned the auth to do "splitVector" action
* https://docs.mongodb.com/v3.2/reference/built-in-roles/#cluster-administration-roles

## Spark
* SparkSQL with Hive (should use spark-submit)
* Submit spark job by akka actor

## Run on production
### build
1. sbt
2. compile
3. assembly

### run
* [Pure Scala APP] java -cp target/scala-2.11/AkkaPubSub-assembly-xx.jar  com.github.tykuo.xx.SomeMainClass
* [Spark APP] spark-submit --class com.github.tykuo.xx.SomeMainClass target/scala-2.11/AkkaPubSub-assembly-xx.jar arg1 
