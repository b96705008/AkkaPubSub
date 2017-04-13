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
import scala.util.Random


object AutoPartitionConsumer {
  def apply(system: ActorSystem, config: Config): ActorRef = {
    val consumerConf = KafkaConsumer.Conf(
      new StringDeserializer,
      new StringDeserializer,
      groupId = "test_group",
      enableAutoCommit = false,
      autoOffsetReset = OffsetResetStrategy.EARLIEST)
      .withConf(config)

    val actorConf = KafkaConsumerActor.Conf(1.seconds, 3.seconds)
    system.actorOf(Props(new AutoPartitionConsumer(List("test"), consumerConf, actorConf)))
  }
}

class AutoPartitionConsumer(topics: List[String],
                            kafkaConfig: KafkaConsumer.Conf[String, String],
                            actorConfig: KafkaConsumerActor.Conf) extends Actor with ActorLogging {

  val recordsExt = ConsumerRecords.extractor[String, String]
  val rand = new Random()

  val consumer = context.actorOf(
    KafkaConsumerActor.props(kafkaConfig, actorConfig, self)
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
  implicit val timeout = Timeout(5 seconds)

  // Consumer
  println("Ready to create consumer!")
  val consumeActor = AutoPartitionConsumer(
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
