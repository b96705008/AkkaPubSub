package com.github.tykuo.kafka

import java.beans.Transient

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import cakesolutions.kafka.{KafkaConsumer, KafkaProducer, KafkaProducerRecord}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetResetStrategy}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import com.typesafe.config.{Config, ConfigFactory}
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}

import scala.concurrent.duration._
import scala.util.Random


object AutoPartitionConsumer {
  def apply(sc: SparkContext, system: ActorSystem, config: Config): ActorRef = {
    val consumerConf = KafkaConsumer.Conf(
      new StringDeserializer,
      new StringDeserializer,
      groupId = "test_group",
      enableAutoCommit = false,
      autoOffsetReset = OffsetResetStrategy.EARLIEST)
      .withConf(config)

    val actorConf = KafkaConsumerActor.Conf(1.seconds, 3.seconds)
    system.actorOf(Props(new AutoPartitionConsumer(sc, consumerConf, actorConf)))
  }
}

class AutoPartitionConsumer(@transient val sc: SparkContext,
                            kafkaConfig: KafkaConsumer.Conf[String, String],
                            actorConfig: KafkaConsumerActor.Conf) extends Actor with ActorLogging {

  val recordsExt = ConsumerRecords.extractor[String, String]
  val rand = new Random()

  val consumer = context.actorOf(
    KafkaConsumerActor.props(kafkaConfig, actorConfig, self)
  )
  context.watch(consumer)

  consumer ! Subscribe.AutoPartition(List("test"))

  override def receive: Receive = {
    case recordsExt(records) =>
      //processRecords(records.pairs)
      //processRecords(records.recordsList)
      processWithSpark(records.recordsList)
      sender() ! Confirm(records.offsets, commit = true)
  }

  private def processRecords(records: Seq[(Option[String], String)]) {
    records.foreach { case (key, value) =>
      log.info(s"Received [$key, $value]")
    }
  }

  private def processRecords(recordsList: List[ConsumerRecord[String, String]]): Unit = {
    recordsList.foreach { r =>
      log.info(s"Received [${r.key()}, ${r.value()}] from topic: ${r.topic()} at ${r.timestamp()}")
    }
  }

  private def processWithSpark(recordsList: List[ConsumerRecord[String, String]]): Unit = {
    recordsList.foreach { r =>
      val v1 = rand.nextInt(10) + 1
      val v2 = rand.nextInt(2) + 1
      val num = sc.parallelize(1 to v1).map(_ * v2).reduce(_ + _)
      println(s"${r.topic()} - ${r.value()}: $num")
    }
  }
}

object RunKafkaActor extends App {
  val system = ActorSystem()
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(5 seconds)

  // config
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("com").setLevel(Level.OFF)
  Logger.getRootLogger.setLevel(Level.OFF)

  // spark
  val conf = new SparkConf().setMaster("local[*]").setAppName("SparkKafkaStream")
  val sc = new SparkContext(conf)

  // Consumer
  println("Ready to create consumer!")
  val consumeActor = AutoPartitionConsumer(
    sc,
    system,
    ConfigFactory.load().getConfig("consumer"))

  // Producer
  val record = KafkaProducerRecord("test", Some("key"), "value")
  val producer = KafkaProducer(
    KafkaProducer.Conf(
      new StringSerializer(),
      new StringSerializer(),
      bootstrapServers = "localhost:9092")
  )
//  system.scheduler.schedule(2 seconds, 2 seconds) {
//    println("send message to topic: test")
//    producer.send(record)
//  }
}
