package com.github.tykuo.component.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future


case class Post(userId: Int, id: Int, title: String, body: String)

object JsonProtocol extends DefaultJsonProtocol {
  implicit val format = jsonFormat4(Post.apply)
}

object RequestClient extends App {
  import JsonProtocol._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val responseFuture: Future[HttpResponse] =
    Http().singleRequest(HttpRequest(uri = "https://jsonplaceholder.typicode.com/posts/1"))

  responseFuture map { res =>
    res.status match {
      case OK =>
        Unmarshal(res.entity).to[Post].map { post =>
          println(s"The post is $post")
        }

      case _ =>
        Unmarshal(res.entity).to[String].map { body =>
          println(s"Ths response status is ${res.status} and response body is $body")
        }
    }
  }
}
