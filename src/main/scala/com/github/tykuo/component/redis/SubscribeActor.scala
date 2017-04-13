package com.github.tykuo.component.redis

import java.net.InetSocketAddress

import akka.actor.ActorRef
import redis.actors.RedisSubscriberActor
import redis.api.pubsub.{Message, PMessage}


class SubscribeActor(channels: Seq[String] = Nil, fsm: ActorRef)
  extends RedisSubscriberActor(
    new InetSocketAddress("localhost", 6379),
    channels,
    Nil,
    onConnectStatus = connected => {println(s"connected: $connected")}
  ) {
  import com.github.tykuo.component.akka.fsm.ServiceFSM._

  override def onMessage(m: Message): Unit = {
    if (m.channel == "time") {
      fsm ! Process(m.data.toString())
    } else if (m.channel == "finish") {
      fsm ! Finish
    }
    //println(s"message received: $m")
  }

  override def onPMessage(pm: PMessage): Unit = {
    println(s"pattern message received: $pm")
  }
}