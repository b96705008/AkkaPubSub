package com.github.tykuo.component.akka.stream

import akka.actor.Props
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.akka.AkkaUtils


object RunAkkaStream extends App {
  // config
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("com").setLevel(Level.OFF)
  Logger.getRootLogger.setLevel(Level.OFF)

  val sparkConf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("RunAkkaStream")
  val ssc = new StreamingContext(sparkConf, Seconds(2))


  val remoteURL = "akka.tcp://AkkaSpark@127.0.0.1:2552/user/feeder"
  val lines = AkkaUtils.createStream[String](
    ssc,
    Props(new SampleActorReceiver[String](remoteURL)),
    "SampleReceiver"
  )

  lines.flatMap(_.split("\\s+")).map((_, 1)).reduceByKey(_ + _).print()

  ssc.start()
  ssc.awaitTermination()
}
