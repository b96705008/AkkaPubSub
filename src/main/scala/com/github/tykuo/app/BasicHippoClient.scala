package com.github.tykuo.app


import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import cakesolutions.kafka.akka.KafkaConsumerActor
import cakesolutions.kafka.{KafkaConsumer, KafkaProducer, KafkaProducerRecord}
import com.github.tykuo.component.kafka.AutoPartitionConsumer
import com.github.tykuo.component.spark.SparkSubmitter
import com.github.tykuo.component.spark.SparkSubmitter.SparkJob
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Await


class BasicHippoClient(hippoName: String,
                       subTopics: List[String],
                       kafkaConfig: KafkaConsumer.Conf[String, String],
                       actorConfig: KafkaConsumerActor.Conf
                      ) extends AutoPartitionConsumer(subTopics, kafkaConfig, actorConfig) {

  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(actorConfig.unconfirmedTimeout)

  val pubTopic = "test"

  // Producer
  val producer = KafkaProducer(
    KafkaProducer.Conf(
      new StringSerializer(),
      new StringSerializer(),
      bootstrapServers = "localhost:9092")
  )

  // Submitter
  val submitter: ActorRef = context.actorOf(
    Props[SparkSubmitter],
    name = "submitter")


  def handleSubmitInAwait(consumerRecord: ConsumerRecord[String, String]): Unit = {
    val future = submitter ? SparkJob("Miles", "python")
    val result = Await.result(future, timeout.duration).asInstanceOf[String]
    println(s"Await result: $result")
    val record = KafkaProducerRecord(pubTopic, Some("spark-job"), s"Spark Job result: $result")
    producer.send(record)

  }

  def handleSubmitInAsync(consumerRecord: ConsumerRecord[String, String]): Unit = {
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
          handleSubmitInAwait(r)
        }
      }
  }
}
