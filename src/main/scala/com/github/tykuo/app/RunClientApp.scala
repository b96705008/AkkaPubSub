package com.github.tykuo.app


import akka.actor.{ActorSystem, Props}
import cakesolutions.kafka.akka.KafkaConsumerActor
import cakesolutions.kafka.KafkaConsumer
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration._


object RunClientApp extends App {
  val timeoutDuration = 10 minutes
  val system = ActorSystem()

  val hippoName = "batch-etl.test"

  val subTopics = List("test")

  val consumerConf = KafkaConsumer.Conf(
    new StringDeserializer,
    new StringDeserializer,
    groupId = hippoName,
    enableAutoCommit = false,
    autoOffsetReset = OffsetResetStrategy.EARLIEST)
    .withConf(ConfigFactory.load().getConfig("consumer"))

  val actorConf = KafkaConsumerActor.Conf(1.seconds, timeoutDuration)

  val client = system.actorOf(
    Props(new BasicHippoClient(hippoName, subTopics, consumerConf, actorConf)),
    name = hippoName)

  println(s"start the hippo: $hippoName")
}