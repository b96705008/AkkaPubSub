package com.github.tykuo.app


import java.io.File

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._


object RunClientApp extends App {
  val timeoutDuration = 10 minutes
  val system = ActorSystem()

  val confPath = if (args.length > 0) args(0) else "config/dev.conf"

  val config = ConfigFactory.parseFile(new File(confPath))
  val hippoName = config.getString("hippo.name")

  if (args.length > 1 && args(1) == "fsm") {
    system.actorOf(Props(new FSMClient(config)), name = hippoName)
  } else {
    system.actorOf(Props(new BasicClient(config)), name = hippoName)
  }

  println(s"start the hippo client: $hippoName")
}