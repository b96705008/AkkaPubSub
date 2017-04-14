package com.github.tykuo.app


import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import cakesolutions.kafka.akka.KafkaConsumerActor
import cakesolutions.kafka.akka.KafkaConsumerActor.Confirm
import cakesolutions.kafka.{KafkaConsumer, KafkaProducer, KafkaProducerRecord}
import com.github.tykuo.component.kafka.AutoPartitionConsumer
import com.github.tykuo.component.spark.SparkSubmitter
import com.github.tykuo.component.spark.SparkSubmitter.SparkJob
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Await
import scala.concurrent.duration._


class BasicClient(val hippoName: String,
                  val subTopics: Array[String],
                  val pubTopic: String,
                  consumerConf: Config,
                  producerConf: Config
                 ) extends AutoPartitionConsumer(subTopics, consumerConf) {


  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(10 minutes)

  // Producer
  val producer = KafkaProducer(
    KafkaProducer.Conf(
      new StringSerializer(),
      new StringSerializer()
    ).withConf(producerConf))

  // Submitter
  val submitter: ActorRef = context.actorOf(
    Props[SparkSubmitter],
    name = "submitter")

  def handleSubmitInAwait(): Unit = {
    val future = submitter ? SparkJob("Miles", "python")
    val result = Await.result(future, timeout.duration).asInstanceOf[String]
    println(s"Await result: $result")
    val record = KafkaProducerRecord(pubTopic, Some("spark-job"), s"Spark Job result: $result")
    producer.send(record)

  }

  def handleSubmitInAsync(): Unit = {
    submitter ? SparkJob("Mike", "python") onSuccess {
      case x: String =>
        println("Got async result: " + x)
        val record = KafkaProducerRecord(pubTopic, Some("spark-job"), s"Spark Job result: $x")
        producer.send(record)
    }
  }

  override protected def processRecords(recordsList: List[ConsumerRecord[String, String]]): Unit = {
    println("processRecords...")
    recordsList
      .foreach { r =>
        log.info(s"Received [${r.key()}, ${r.value()}] from topic: ${r.topic()} at ${r.timestamp()}")
        if (r.value() == "submit") {
          handleSubmitInAwait()
        }
      }
  }
}
