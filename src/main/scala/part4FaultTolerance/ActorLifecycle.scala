package part4FaultTolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}

object ActorLifecycle extends App {

  object StartChild

  class LifecycleActor extends Actor with ActorLogging {

    override def preStart() = log.info("I am starting")

    override def postStop() = log.info("I have stopped")

    override def receive: Receive = {
      case StartChild => context.actorOf(Props[LifecycleActor], "child")
    }
  }

  val system = ActorSystem("LifecycleDemo")
//
//  val parent = system.actorOf(Props[LifecycleActor], "parent")
//
//  parent ! StartChild
//
//  parent ! PoisonPill

  // restart hooks

  object Fail
  object FailChild
  object CheckChild
  object Check

  class Parent extends Actor {
    private val child = context.actorOf(Props[Child], "supervisedChild")
    override def receive: Receive = {
      case FailChild =>
        child ! Fail
      case CheckChild =>
        child ! Check
    }
  }

  class Child extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("supervised child started")
    override def postStop(): Unit = log.info("supervised child stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info(s"Supervised child restarting because of ${reason.getMessage}")

    override def postRestart(reason: Throwable): Unit = log.info("Supervised child restarted")

    override def receive: Receive = {
      case Fail =>
        log.warning("child will fail now")
        throw new RuntimeException("I failed")
      case Check =>
        log.info("alive and kicking")
    }
  }

  val supervisor = system.actorOf(Props[Parent], "supervisor")

  supervisor ! FailChild
  supervisor ! CheckChild

  // default supervison strategy
}
