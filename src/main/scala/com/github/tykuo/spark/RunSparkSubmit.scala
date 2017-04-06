package com.github.tykuo.spark

import sys.process._
import sys.process.Process
import akka.actor.{Actor, ActorSystem, Props}
import SparkSubmitter._
import cakesolutions.kafka.{KafkaProducer, KafkaProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

object SparkSubmitter {
  case class SparkJob(name: String, fileType: String)
}


class SparkSubmitter extends Actor {
  val producer = KafkaProducer(
    KafkaProducer.Conf(
      new StringSerializer(),
      new StringSerializer(),
      bootstrapServers = "localhost:9092")
  )

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
    case SparkJob(name, fileType) =>
      println(s"I just got a job from $name")
      val command = if (fileType == "python") getSubmitPyCommand else getSumbitJarCommand
      println(command.mkString(" "))

      var message = "running"
      val result = command.!

      if (result == 0) {
        message = "Finish spark job finish successfully."
      } else {
        message = "Stop spark job with error!"
      }

      message = message + ", user is " + name
      println(message)
      val record = KafkaProducerRecord("test", Some("spark-job"), message)
      producer.send(record)
  }
}


object RunSparkSubmit extends App {
  val system = ActorSystem()

  val submitter = system.actorOf(Props[SparkSubmitter], name = "submitter")
  //submitter ! SparkJob("Roger", "jar")
  submitter ! SparkJob("Miles", "python")

  println("Waiting for running!")
  //Thread.sleep(1000)
  //system.terminate()
}
