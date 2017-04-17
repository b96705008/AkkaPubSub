package com.github.tykuo.app


import akka.actor.{ActorRef, FSM, Props}
import cakesolutions.kafka.akka.KafkaConsumerActor.Confirm
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.duration._


object FSMService {
  // FSM State
  sealed trait State
  case object Idle extends State
  case object Waiting extends State
  case object Submitting extends State

  // FSM Data
  case class Status(msgSet: Set[String])
  def EmptyStatus = Status(Set())

  // Event
  case class GetMsg(msg: String)
  case object Finish
  case object Cancel
  case object Submit
}

class FSMService(needTables: Set[String]) extends FSM[FSMService.State, FSMService.Status] {

  import FSMService._

  def isReady(s: Status): Boolean =
    (needTables -- s.msgSet).isEmpty

  def addTableToStatus(msg: String): (Status, Boolean) = {
    if (needTables.contains(msg)) {
      (Status(stateData.msgSet + msg), true)
    } else {
      (stateData, false)
    }
  }

  // 1. define start status
  startWith(Idle, EmptyStatus)
  println("start with idle")

  // 2. define states
  when(Idle) {
    case Event(GetMsg(msg), _) =>
      val (status, isNew) = addTableToStatus(msg)
      println(status.msgSet.mkString(", "))

      if (isReady(status)) {
        goto(Submitting) using Status(needTables)
      } else if (!isNew) {
        stay using status
      } else {
        goto(Waiting) using status
      }
  }

  when(Waiting, stateTimeout = 5 minutes) {
    case Event(GetMsg(msg), _) =>
      val (status, _) = addTableToStatus(msg) //Status(stateData.msgSet + msg)
      println(status.msgSet.mkString(", "))

      if (isReady(status)) {
        goto(Submitting) using Status(needTables)
      } else {
        stay using status
      }

    case Event(Cancel, _) =>
      goto(Idle) using EmptyStatus

    case Event(StateTimeout, _) => {
      println("timeout!")
      goto(Idle) using EmptyStatus
    }
  }

  when(Submitting) {
    case Event(Finish, _) =>
      goto(Idle) using EmptyStatus
  }

  onTransition {
    case Idle -> Waiting =>
      println("Go Waiting!")
    case Waiting -> Idle =>
      println("Wait too long... Go back to Idle.")
    case _ -> Submitting =>
      println(s"Get all the data and start working.")
      context.parent ! Submit
    case Submitting -> Idle =>
      println("Finish! go back to idle.")
  }

  // 3. init
  initialize()
}

class FSMClient(config: Config) extends BasicClient(config) {

  // config
  val needTables: Set[String] = config
    .getStringList("hippo.need-tables").toArray.map(_.toString).toSet

  import FSMService._

  val fsmService: ActorRef = context.actorOf(
    Props(new FSMService(needTables)), name = "fsm-service")

  override protected def processRecords(recordsList: List[ConsumerRecord[String, String]]): Unit = {
    recordsList
      .foreach { r =>
        log.info(s"Received [${r.key()}, ${r.value()}] from topic: ${r.topic()}}")
        fsmService ! GetMsg(r.value())
      }
  }

  override def receive: Receive = {
    case recordsExt(records) =>
      processRecords(records.recordsList)
      sender() ! Confirm(records.offsets, commit = true)

    case Submit =>
      println("receive Submit command!...")
      handleSubmitInAwait()
      fsmService ! Finish
  }
}
