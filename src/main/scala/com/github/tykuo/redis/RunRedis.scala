package com.github.tykuo.redis


import redis.RedisClient
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object RunRedis extends App {
  implicit val system = akka.actor.ActorSystem()

  val redis = RedisClient()
  val futurePong = redis.ping()
  println("Ping sent!")
  futurePong.map { pong =>
    println(s"Redis replied with a $pong")
  }
  Await.result(futurePong, 5 seconds)

  system.terminate()
}
