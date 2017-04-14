package com.github.tykuo.component.kafka

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import cakesolutions.kafka.akka.KafkaConsumerActor.{Confirm, Subscribe}
import cakesolutions.kafka.akka.{ConsumerRecords, KafkaConsumerActor}
import cakesolutions.kafka.{KafkaConsumer, KafkaProducer, KafkaProducerRecord}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.{ConsumerRecord, OffsetResetStrategy}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.duration._


class AutoPartitionConsumer(config: Config) extends Actor with ActorLogging {

  // config
  private val consumerConf = config.getConfig("kafka.consumer")
  val topics: Array[String] = config
    .getStringList("hippo.subscribe-topics").toArray.map(_.toString)

  // consumer
  val recordsExt = ConsumerRecords.extractor[String, String]

  val consumer: ActorRef = context.actorOf(
    KafkaConsumerActor.props(
      consumerConf,
      new StringDeserializer,
      new StringDeserializer,
      self)
  )
  context.watch(consumer)

  consumer ! Subscribe.AutoPartition(topics)

  override def receive: Receive = {
    case recordsExt(records) =>
      processRecords(records.recordsList)
      sender() ! Confirm(records.offsets, commit = true)
  }

  protected def processRecords(recordsList: List[ConsumerRecord[String, String]]): Unit = {
    recordsList.foreach { r =>
      log.info(s"Received [${r.key()}, ${r.value()}] from topic: ${r.topic()} at ${r.timestamp()}")
    }
  }
}

object RunKafkaActor extends App {
  val system = ActorSystem()
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(5 seconds)

  val config = ConfigFactory.load()

  // Consumer
  println("Ready to create consumer!")
  val consumerActor = system.actorOf(
    Props(new AutoPartitionConsumer(config)))

  // Producer
  val producerConf = config.getConfig("kafka.producer")
  val record = KafkaProducerRecord("test", Some("key"), "value")
  val producer = KafkaProducer(
    KafkaProducer.Conf(
      new StringSerializer(),
      new StringSerializer()
    ).withConf(producerConf))

  system.scheduler.schedule(2 seconds, 2 seconds) {
    println("send message to topic: test")
    producer.send(record)
  }
}
