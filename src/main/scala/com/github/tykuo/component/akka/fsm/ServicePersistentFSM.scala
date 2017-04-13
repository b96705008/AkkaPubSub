package com.github.tykuo.component.akka.fsm

import akka.actor.{ActorSystem, Props}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import com.github.tykuo.component.akka.fsm.ServicePersistentFSM._

import scala.concurrent.duration._
import scala.reflect._


object ServicePersistentFSM {
  // State
  sealed trait State extends FSMState
  case object Idle extends State {
    override def identifier = "Idle"
  }

  case object Waiting extends State {
    override def identifier = "Waiting"
  }

  case object Working extends State {
    override def identifier = "Working"
  }

  // Data
  sealed trait Data {
    val status: Int
    def isCompleted: Boolean
    def addStatus(): Data
    def refresh: Data
  }

  case class UncompletedData(maxStatus: Int, initStatus: Int = 0) extends Data {
    override val status: Int = initStatus

    override def isCompleted: Boolean = false

    override def addStatus(): Data = {
      val newStatus = status + 1
      if (newStatus >= maxStatus) {
        CompletedData(newStatus)
      } else {
        UncompletedData(maxStatus, newStatus)
      }
    }

    override def refresh = UncompletedData(maxStatus)
  }

  case class CompletedData(initStatus: Int) extends Data {
    override val status: Int = initStatus

    override def isCompleted = true

    override def addStatus(): CompletedData = this

    override def refresh = UncompletedData(status)
  }

  // Domain Events (Persist events)
  sealed trait DomainEvent
  case object AddStatus extends DomainEvent
  case object Refresh extends DomainEvent

  // Command
  sealed trait Command
  case class Dispatch(msg: String) extends Command
  case object Finish extends Command
}


class ServicePersistentFSM(maxStatus: Int) extends PersistentFSM[State, Data, DomainEvent] {


  override def domainEventClassTag: ClassTag[DomainEvent] = classTag[DomainEvent]

  override def persistenceId: String = "hippo"

  override def applyEvent(evt: DomainEvent, currentData: Data): Data = {
    evt match {
      case AddStatus =>
        val newData = currentData.addStatus()
        println(s"New status: ${newData.status}, isCompleted: ${newData.isCompleted}")
        newData

      case Refresh =>
        val newData = currentData.refresh
        println(s"New status: ${newData.status}, isCompleted: ${newData.isCompleted}")
        newData
    }
  }

  // FSM
  startWith(Idle, UncompletedData(maxStatus))
  println("start with idle.")

  when(Idle) {
    case Event(Dispatch(m), _) =>
      println(s"Got the message: $m")
      val newData = stateData.addStatus()
      if (newData.isCompleted) {
        goto(Working) applying AddStatus
      } else {
        goto(Waiting) applying AddStatus
      }
  }

  when(Waiting) {
    case Event(Dispatch(_), _) =>
      val newData = stateData.addStatus()
      if (newData.isCompleted) {
        goto(Working) applying AddStatus
      } else {
        println(s"Wait and status is ${newData.status}")
        stay applying AddStatus
      }
  }

  when(Working) {
    case Event(Finish, _) =>
      println("Working with status: " + stateData.status)
      goto(Idle) applying Refresh
  }

  onTransition {
    case Idle -> Waiting =>
      println("Go Waiting!")
    case Waiting -> Working =>
      println(s"Get all the data and start working.")
      setTimer("timeout", Finish, 3 seconds)
    case Working -> Idle =>
      println("Finish! go back to idle.")
  }
}

object TestPersistFSM extends App {
  val system = ActorSystem("persistent-fsm")

  val serviceFSM = system.actorOf(Props(new ServicePersistentFSM(10)), name = "service-fsm")

  for (i <- 1 to 7) {
    serviceFSM ! Dispatch("some message")
  }

  for (i <- 1 to 10) {
    serviceFSM ! Dispatch("some message")
  }
  serviceFSM ! Finish

  Thread.sleep(4000)
  system.terminate()
}