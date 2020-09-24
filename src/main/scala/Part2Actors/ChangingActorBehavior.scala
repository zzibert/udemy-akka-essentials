package Part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.collection.mutable.Map

object ChangingActorBehavior extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class StateLessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender ! KidAccept
    }

    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false)
      case Food(CHOCOLATE) => context.unbecome()
      case Ask(_) => sender ! KidReject
    }
  }

  object Mom {
    case class MomStart(kid: ActorRef)
    case class GiveChocolate(kid: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // do you want to play
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor {
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case KidAccept => println("yey my kid is happy")
      case KidReject => println("my kid is sad, but he is healthy")
      case MomStart(kid) =>
        kid ! Food(VEGETABLE)
        kid ! Food(VEGETABLE)
        kid ! Food(CHOCOLATE)
        kid ! Food(CHOCOLATE)
        kid ! Ask("Do you want to play")
      case GiveChocolate(kid) =>
        kid ! Food(CHOCOLATE)
        kid ! Ask("do you want to play")

    }
  }

  // 1. recreate Counter Actor with context.become and no mutable state

  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }

  class CounterActor extends Actor {
    import CounterActor._

    override def receive: Receive = zero

    def zero: Receive = {
      case Increment => context.become(increment(1), false)
      case Decrement => context.become(decrement(-1), false)
      case Print => println(s"the count is 0")
    }

    def increment(count: Int): Receive = {
      case Increment => context.become(increment(count+1), false)
      case Decrement => context.unbecome()
      case Print => println(s"the count is $count")
    }

    def decrement(count: Int): Receive = {
      case Increment => context.unbecome()
      case Decrement => context.become(decrement(count-1), false)
      case Print => println(s"the count is $count")
    }
  }

  import CounterActor._

  val system = ActorSystem("ActorSystem")
  val counter = system.actorOf(Props[CounterActor])
  counter ! Print
  counter ! Decrement
  counter ! Decrement
  counter ! Decrement
  counter ! Print
  counter ! Increment
  counter ! Print

  // 2. simplified voting system

  object Citizen {
    case object VoteStatusRequest
    case class Vote(Candidate: String)
    case class VoteStatusReply(Candidate: Option[String])
  }

  class Citizen extends Actor {
    import Citizen._
    override def receive: Receive = notVoted

    def notVoted: Receive = {
      case Vote(candidate) => context.become(voted(candidate))
      case VoteStatusRequest => sender ! VoteStatusReply(None)
    }

    def voted(candidate: String): Receive = {
      case Vote(_) => println("this citizen has already voted")
      case VoteStatusRequest => sender ! VoteStatusReply(Some(candidate))
    }
  }

  object VoteAggregator {
    case class AggregateVotes(citizens: Set[ActorRef])
    case class PrintVotes(size: Int)
  }

  class VoteAggregator extends Actor {
    import VoteAggregator._
    import Citizen._
    var votes = Map[String, Int]().withDefaultValue(0)

    override def receive: Receive = voteReceiver(votes, 0)

    def voteReceiver(map: Map[String, Int], count: Int): Receive = {
      case AggregateVotes(citizens) => {
        citizens.foreach(citizen => citizen ! VoteStatusRequest)
        self ! PrintVotes(citizens.size)
      }
      case VoteStatusReply(Some(candidate)) => {
        println("received reply")
        votes(candidate) += 1
        context.become(voteReceiver(votes, count + 1))
      }
      case VoteStatusReply(None) => sender ! VoteStatusRequest

      case PrintVotes(size) => {
        if (size == count) for ((candidate, votes) <- votes) println(s"$candidate -> $votes")
        else self ! PrintVotes(size)
      }
    }
  }

  import Citizen._
  import VoteAggregator._

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))


  // Print the status of the votes
  // map of every candidate and the number of votes they received
  // Martin -> 1
  // Jonas -> 1
  // Roland -> 2

}
