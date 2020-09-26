package Part2Actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {

  // #1 Explicit Logging
  class SimpleActorWithExplicitLogger extends Actor {
    val logger = Logging(context.system, this)
    override def receive: Receive = {
            // 1- debug
            // 2 - info
            // 3 - warn
            // 4 - error
      case message => logger.info(message.toString)// LOG IT
    }
  }
  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogger])

  actor ! "Logging a simple message"

  // #2 - ActorLogging
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("two things: {} and {}", a, b) // interpolate
      case message => log.info(message.toString)
    }
  }

  val simplerActor = system.actorOf(Props[ActorWithLogging])

  simplerActor ! "logging a simple message by extending a trait"

  simplerActor ! (42, 65)
}
