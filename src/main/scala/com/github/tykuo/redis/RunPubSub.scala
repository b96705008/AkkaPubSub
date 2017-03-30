package com.github.tykuo.redis


import akka.actor.Props
import akka.util.Timeout
import com.github.tykuo.akka.fsm.ServiceFSM
import redis.{RedisClient, RedisDispatcher}

import scala.concurrent.duration._


object RunPubSub extends App {
  implicit val system = akka.actor.ActorSystem()
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val redisDispatcher = RedisDispatcher("akka.actor.default-dispatcher")
  implicit val timeout = Timeout(5 seconds)

  val redis = RedisClient()

//  system.scheduler.schedule(2 seconds, 2 seconds) {
//    redis.publish("time", System.currentTimeMillis())
//  }
  //system.scheduler.scheduleOnce(20 seconds)(system.terminate())

  val serviceStorage = system.actorOf(Props[ServiceFSM], "service-fsm")
  val subscribeActor = system.actorOf(
    Props(classOf[SubscribeActor], Seq("time", "finish"), serviceStorage),
    "subscriber")

  println("wait for terminating ...")
}


