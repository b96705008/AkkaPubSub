package com.github.tykuo.app


import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import com.github.tykuo.component.kafka.AutoPartitionConsumer
import com.github.tykuo.component.spark.SparkSubmitter
import com.github.tykuo.component.spark.SparkSubmitter.SparkJob
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import spray.json._


class BasicClient(config: Config) extends AutoPartitionConsumer(config) {
  import HippoJsonProtocol._
  import HippoUtils._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(10 minutes)

  // config
  val hippoName: String = config.getString("hippo.name")
  private val producerConf = config.getConfig("kafka.producer")
  val pubTopic: String = config.getString("hippo.publish-topic")
  val bashPath: String = config.getString("spark.bash-path")

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

  def publishJobFinishMsg(isSuccess: Boolean): Unit = {
    val message = JobFinishMessage(
      hippoName,
      "testing_spark_job",
      isSuccess,
      currentTimestamp).toJson.prettyPrint

    println("finish and publish record: " + message)
    val record = KafkaProducerRecord(pubTopic, Some("spark-job"), message)
    producer.send(record)
  }

  def handleSubmitInAwait(): Unit = {
    val future = (submitter ? SparkJob(bashPath)).mapTo[Boolean]
    val isSuccess = Await.result(future, timeout.duration)
    publishJobFinishMsg(isSuccess)
  }

  def handleSubmitInAsync(): Unit = {
    submitter ? SparkJob(bashPath) onSuccess {
      case isSuccess: Boolean => publishJobFinishMsg(isSuccess)
    }
  }

  override protected def processRecords(recordsList: List[ConsumerRecord[String, String]]): Unit = {
    recordsList
      .foreach { r =>
        log.info(s"Received [${r.key()}, ${r.value()}] from topic: ${r.topic()}")

        r.topic() match {
          case TEST_SUBMIT_MSG =>
            if (r.value() == "test-submit") {
              handleSubmitInAwait()
            }

          case FRONTIER_MSG =>
            try {
              val msg = r.value().parseJson.convertTo[FrontierMessage]
              println(s"process ${msg.db}.${msg.table}")
              handleSubmitInAwait()
            } catch {
              case e: Exception =>
                println(e)
                println(s"parse ${r.value()} fail...")
            }
        }
      }
  }
}
