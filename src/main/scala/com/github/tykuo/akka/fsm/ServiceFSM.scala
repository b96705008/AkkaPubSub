package com.github.tykuo.akka.fsm

import akka.actor.{FSM, Stash}

import scala.concurrent.duration._

object ServiceFSM {

  // FSM State
  sealed trait State
  case object Idle extends State
  case object Waiting extends State
  case object Working extends State

  // FSM Data
  case class Status(cnt: Int)

  // Event
  case object Cancel
  case class Process(msg: String)
  case object Finish
}


class ServiceFSM extends FSM[ServiceFSM.State, ServiceFSM.Status] with Stash {
  import ServiceFSM._

  // 1. define start status
  startWith(Idle, Status(0))
  println("start with idle.")

  // 2. define states
  when(Idle) {
    case Event(Process(m), _) =>
      println(s"Got the message: $m")
      //unstashAll()
      goto(Waiting) using Status(1)

//    case Event(_, _) =>
//      stash()
//      stay using stateData
  }

  when(Waiting) {
    case Event(Process(m), _) =>
      cancelTimer("timeout")
      val cnt = stateData.cnt + 1
      if (cnt >= 10) {
        goto(Working) using Status(cnt)
      } else {
        println(s"Wait and status is $cnt")
        setTimer("timeout", Cancel, 10 seconds, repeat = false)
        stay using Status(cnt)
      }
    case Event(Cancel, _) =>
      goto(Idle) using Status(0)
  }

  when(Working) {
    case Event(Finish, _) =>
      println("Work with data cnt: " + stateData.cnt)
      goto(Idle) using Status(0)

    case Event(_, _) =>
      println("Stay in working status.")
      stay using stateData
  }

  onTransition {
    case Idle -> Waiting =>
      println("Go Waiting!")
      setTimer("timeout", Cancel, 10 seconds, repeat = false)
    case Waiting -> Idle =>
      println("Wait too long... Go back to Idle.")
    case Waiting -> Working =>
      println(s"Get all the data and start working.")
    case Working -> Idle =>
      println("Finish! go back to idle.")
  }

  // 3. init
  initialize()
}
