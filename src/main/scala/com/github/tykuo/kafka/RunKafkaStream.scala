package com.github.tykuo.kafka

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.{Level, Logger}
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._



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

  val topics = Array("test")
  val stream = KafkaUtils.createDirectStream[String, String](
    ssc, PreferConsistent, Subscribe[String, String](topics, kafkaParams)
  )

  val records = stream.map(record => (record.key, record.value))
  records.print()

  ssc.start()
  ssc.awaitTermination()
}
