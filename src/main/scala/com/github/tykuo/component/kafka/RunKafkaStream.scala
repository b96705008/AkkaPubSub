package com.github.tykuo.component.kafka

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Created by roger19890107 on 11/04/2017.
  */
object RunKafkaStream extends App {
  // config
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("com").setLevel(Level.OFF)
  Logger.getRootLogger.setLevel(Level.OFF)

  // spark
  val conf = new SparkConf().setMaster("local[*]").setAppName("SparkKafkaStream")
  val ssc = new StreamingContext(conf, Seconds(1))

  // kafka
  val kafkaParams = Map[String, Object](
    "bootstrap.servers" -> "localhost:9092",
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "spark_streaming",
    "auto.offset.reset" -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean)
  )

  val numThreads = 1
  val topics = "roger-test"
  val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap
  val stream = KafkaUtils.createStream(
    ssc, "localhost:2181", "test_group", topicMap
  )

  val records = stream.map(_._2)
  records.print()

  ssc.start()
  ssc.awaitTermination()
}
