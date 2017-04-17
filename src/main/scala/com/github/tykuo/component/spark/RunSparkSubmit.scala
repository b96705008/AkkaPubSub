package com.github.tykuo.component.spark

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import com.github.tykuo.component.spark.SparkSubmitter._
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.sys.process._

object SparkSubmitter {
  case class SparkJob(bashPath: String)
}


class SparkSubmitter extends Actor {

  def getSubmitCommand(path: String): Seq[String] =
    Seq("/bin/bash", path)


  override def receive: Receive = {
    case SparkJob(bashPath) =>
      println(s"I just got a job to run $bashPath")
      val result = getSubmitCommand(bashPath).!

      if (result == 0) {
        sender ! true
      } else {
        sender ! false
      }
  }
}


object RunSparkSubmit extends App {
  implicit val timeout = Timeout(30 seconds)
  import scala.concurrent.ExecutionContext.Implicits.global

  val bashPath = "/Users/roger19890107/Developer/main/projects/cathay/hippo/AkkaPubSub/scripts/py-submit.sh"
  val producer = KafkaProducer(
    KafkaProducer.Conf(
      new StringSerializer(),
      new StringSerializer(),
      bootstrapServers = "localhost:9092")
  )

  val system = ActorSystem()
  val submitter = system.actorOf(Props[SparkSubmitter], name = "submitter")

  submitter ? SparkJob(bashPath) onSuccess {
    case x: String =>
      println("Is success? " + x)
      val record = KafkaProducerRecord("test", Some("spark-job"), x)
      producer.send(record)
  }

  println("Waiting for running!")
}
