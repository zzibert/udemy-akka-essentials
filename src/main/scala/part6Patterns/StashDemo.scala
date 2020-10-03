package part6Patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  // Resource actor
  // - open => it can receive read/write request to the resource
  // - otherwise it will postpone all read/write requests until the state is open
  // resource actor is Close
  // - Open => switch to Open state
  // - Read, Write messages are postponed
  // it can handle read and write messages
  // - Close => switch to Close state
  // [Open, Read, Read, Write]
  // [Read, Open, Write]
  // - stash Read Stash: [Read]
  // open => switch to the open state => stash is prepended to the mailbox
  // read and write are handled

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  // mixin the stash trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource")
        // unstashAll when you switch the message handler
        unstashAll()
        context.become(open)

      case message =>
        log.info(s"stashing message $message, because i cant handle it in the closed state")
        // stash away messages you cant handle
        stash
    }

    def open: Receive = {
      case Read =>
        // do computation
        log.info(s"I have read the $innerData")
      case Write(data) =>
        log.info(s"I have writing $data")
        innerData = data
      case Close =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)
      case message =>
        log.info(s"stashing message $message, because i cant handle it in the open state")
        // stash away messages you cant handle
        stash
    }
  }

  val system = ActorSystem("StashDemo")

  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Read
  resourceActor ! Open
  resourceActor ! Open
  resourceActor ! Write("I love stash")
  resourceActor ! Close
  resourceActor ! Read

}
