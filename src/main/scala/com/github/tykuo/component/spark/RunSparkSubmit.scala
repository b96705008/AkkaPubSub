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


  def getSubmitPyCommand: Seq[String] = {
    Seq(
      "spark-submit",
      "/Users/roger19890107/Developer/main/projects/cathay/hippo/AkkaPubSub/scripts/spark.py"
    )
  }

  def getSumbitJarCommand: Seq[String] = {
    Seq(
      "spark-submit",
      "--class com.github.tykuo.spark.RunHiveSQL",
      "/Users/roger19890107/Developer/main/projects/cathay/hippo/AkkaPubSub/target/scala-2.11/AkkaPubSub-assembly-1.0.jar"
    )
  }

  override def receive: Receive = {
    case SparkJob(bashPath) =>
      println(s"I just got a job to run $bashPath")
      var message = "running"
      val result = getSubmitCommand(bashPath).!

      if (result == 0) {
        message = "Finish spark job finish successfully."
      } else {
        message = "Stop spark job with error!"
      }
      message = message + ", from bash: " + bashPath
      println(message)
      sender ! message
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
      println("Got some result: " + x)
      val record = KafkaProducerRecord("test", Some("spark-job"), x)
      producer.send(record)
  }

  println("Waiting for running!")
}
