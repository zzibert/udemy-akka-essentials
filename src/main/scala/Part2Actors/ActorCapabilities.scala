package Part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App{

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "hi!" => context.sender ! "hello there!"
      case message: String => println(s"[${self}] I have received $message from $sender")
      case number: Int => println(s" [${context.self}] I have received a number $number")
      case SpecialMessage(contents) => println(s"[${context.self}] I have received a special message: $contents")
      case SendMessageToYourself(content) =>
        self ! content
      case SayHiTo(ref) => ref ! "hi!"
      case WirelessPhoneMessage(content, ref) => ref forward (content + "s") // i keep the original sender
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "hello actor"

  // messages can be of any type
  // Messages must be IMMUTABLE
  // messages must be SERIALIZABLE

  // IN practice use case classes and case objects

  simpleActor ! 42

  case class SpecialMessage(contents: String)

  simpleActor ! SpecialMessage("some special content")

  // Actors have information about their context and about themselves
  // context.self === 'this' in OOP

  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and i am proud of it")

  // 3 actors can reply to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)

  alice ! SayHiTo(bob)

  // 4 - dead letters
  alice ! "hi!"

  // 5 - forwarding messages
  // D -> A -> B === forwarding with the original sender
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("HI", bob)

}
