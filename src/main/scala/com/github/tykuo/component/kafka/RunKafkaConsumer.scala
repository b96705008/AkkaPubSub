package com.github.tykuo.component.kafka

import java.util.{Collections, Properties}

import kafka.utils.Logging
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import spray.json._

import scala.collection.JavaConversions._
import scala.util.Random


case class HippoMessage(table: String, method: String, timestamp: Option[Int])

object HippoJsonProtocol extends DefaultJsonProtocol {
  implicit val hippoMessageFormat = jsonFormat3(HippoMessage)
}

class SparkJobConsumer(val brokers: String,
                       val groupId: String,
                       val topic: String) extends Logging {

  import HippoJsonProtocol._

  val props = createConsumerConfig(brokers, groupId)
  val consumer = new KafkaConsumer[String, String](props)
  val rand = new Random()

  def createConsumerConfig(brokers: String, groupId: String): Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
    props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props
  }

  def processRecordWithSpark(sc: SparkContext, r: ConsumerRecord[String, String]): Unit = {
    println(s"Received [${r.key()}, ${r.value()}] from topic: ${r.topic}")
    val v1 = rand.nextInt(10) + 1
    val v2 = rand.nextInt(2) + 1
    val num = sc.parallelize(1 to v1).map(_ * v2).reduce(_ + _)
    println(s"${r.topic} - ${r.value()}: $num")
  }

  def processRecordWithJson(r: ConsumerRecord[String, String]): Unit = {
    try {
      val jsonAst = r.value().parseJson
      println(jsonAst)
      val hippoMsg = jsonAst.convertTo[HippoMessage]
      println(hippoMsg)
    } catch {
      case ex: Exception =>
        println(ex)
    }
  }

  def run(sc: SparkContext): Unit = {
    println("Start run SparkJobConsumer...")
    consumer.subscribe(Collections.singletonList(this.topic))

    while (true) {
      val records = consumer.poll(1000)
      records.foreach { r =>
        //processRecordWithSpark(sc, r)
        processRecordWithJson(r)
      }
    }
  }
}

object RunKafkaConsumer extends App {
  // config
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("com").setLevel(Level.OFF)
  Logger.getRootLogger.setLevel(Level.OFF)

  // spark
  val conf = new SparkConf().setMaster("local[*]").setAppName("SparkKafkaStream")
  val sc = new SparkContext(conf)

  val jobConsumer = new SparkJobConsumer("localhost:9092", "test_group", "roger-test")
  jobConsumer.run(sc)

}
