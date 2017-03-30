package com.github.tykuo.akka.stream

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.apache.spark.streaming.akka.ActorReceiver

import scala.collection.mutable
import scala.util.Random

case class SubscribeReceiver(receiverActor: ActorRef)
case class UnsubscribeReceiver(receiverActor: ActorRef)


class FeederActor extends Actor {
  val rand = new Random()
  val receivers = new mutable.LinkedHashSet[ActorRef]()

  val strings = Array("words", "may ", "count ")

  def makeMessage(): String = {
    val x = rand.nextInt(3)
    strings(x) + strings(2 - x)
  }

  // async make message
  new Thread() {
    override def run(): Unit = {
      while (true) {
        Thread.sleep(500)
        receivers.foreach(_ ! makeMessage())
      }
    }
  }.start()

  override def receive: Receive = {
    case SubscribeReceiver(receiverActor: ActorRef) =>
      println(s"received subscribe from ${receiverActor.toString}")
      receivers += receiverActor

    case UnsubscribeReceiver(receiverActor: ActorRef) =>
      println(s"received unsubscribe from ${receiverActor.toString}")
      receivers -= receiverActor

    case _ =>
      println("No use!")
  }
}

class SampleActorReceiver[T](urlOfPublisher: String) extends ActorReceiver {
  lazy private val remotePublisher = context.actorSelection(urlOfPublisher)

  override def preStart(): Unit =
    remotePublisher ! SubscribeReceiver(context.self)

  override def receive: PartialFunction[Any, Unit] = {
    case msg => store(msg.asInstanceOf[T])
  }

  override def postStop(): Unit =
    remotePublisher ! UnsubscribeReceiver(context.self)
}

object FeederActor extends App {
  val config = ConfigFactory.load.getConfig("akka-spark")
  val system = ActorSystem("AkkaSpark", config)
  val feeder = system.actorOf(Props[FeederActor], "feeder")

  println("Feeder started as: " + feeder)
}

