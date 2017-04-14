package com.github.tykuo.app


import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._


object RunClientApp extends App {
  val timeoutDuration = 10 minutes
  val system = ActorSystem()

  val config = ConfigFactory.load()
  val hippoName = "batch-etl.test"
  val subTopics = config.getStringList("hippo.subscribe-topics").toArray.map(_.toString)
  val pubTopic = config.getString("hippo.publish-topic")

  val consumerConf = ConfigFactory.load().getConfig("kafka.consumer")
  val producerConf = ConfigFactory.load().getConfig("kafka.producer")

//  val basicClient = system.actorOf(
//    Props(new BasicClient(
//      hippoName,
//      subTopics,
//      consumerConf,
//      actorConf,
//      producerConf)),
//    name = hippoName)

  val needTables = Set("A", "B", "C")
  val fsmClient = system.actorOf(
    Props(new FSMClient(
      hippoName,
      subTopics,
      pubTopic,
      needTables,
      consumerConf,
      producerConf)),
    name = hippoName)

  println(s"start the hippo: $hippoName")
}