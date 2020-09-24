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

//  simpleActor ! "hello actor"

  // messages can be of any type
  // Messages must be IMMUTABLE
  // messages must be SERIALIZABLE

  // IN practice use case classes and case objects

//  simpleActor ! 42

  case class SpecialMessage(contents: String)

//  simpleActor ! SpecialMessage("some special content")

  // Actors have information about their context and about themselves
  // context.self === 'this' in OOP

  case class SendMessageToYourself(content: String)
//  simpleActor ! SendMessageToYourself("I am an actor and i am proud of it")

  // 3 actors can reply to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)

//  alice ! SayHiTo(bob)

  // 4 - dead letters
//  alice ! "hi!"

  // 5 - forwarding messages
  // D -> A -> B === forwarding with the original sender
  case class WirelessPhoneMessage(content: String, ref: ActorRef)
//  alice ! WirelessPhoneMessage("HI", bob)

  // 1. Create counter Actor
  // -increment, decremet, print messages

  // DOMAIN OF THE COUNTERACTOR
  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }

  class CounterActor extends Actor {
    import CounterActor._

    var state: Int = 0
    override def receive: Receive = {
      case Increment => state += 1
      case Decrement => state -= 1
      case Print => println(s"this is the state: $state")
    }
  }

  import CounterActor._

  val counterActor = system.actorOf(Props[CounterActor], "counterActor")

  (1 to 4).foreach(_ => counterActor ! Increment)
  counterActor ! Print
  (1 to 3).foreach(_ => counterActor ! Decrement)
  counterActor ! Print

  // 2. Create BankAccount as an Actor
  // deposit or withdraw amount, statement
  // replying with succes or failure
  // design the messages themseves and the logic for the bankAccount actor
  // interact with some other kind of actor


  object BankAccountActor {
    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
    case object Statement
    case class Deposit(amount: Int)
    case class AccountDeposit(bankAccount: ActorRef, amount: Int)
    case class Withdraw(amount: Int)
    case class AccountWithdraw(bankAccount: ActorRef, amount: Int)
    case class AccountStatement(bankAccount: ActorRef)
  }

  class BankAccountActor extends Actor {
    import BankAccountActor._
    var state: Int = 0
    override def receive: Receive = {
      case Deposit(amount) => {
        if (amount < 0) sender ! TransactionFailure("amount is less than 0")
        else {
          state += amount
          sender ! TransactionSuccess(s"Succesfully added $amount")
        }
      }
      case Withdraw(amount) => {
        if (amount > state) sender ! TransactionFailure("The amount is bigger than the funds")
        else {
          state -= amount
          sender ! TransactionSuccess(s"Succesfully withdrawn $amount")
        }
      }
      case Statement => sender ! state
    }
  }

  class BankCustomer extends Actor {
    import BankAccountActor._
    override def receive: Receive = {
      case AccountDeposit(bankAccount, amount) => bankAccount ! Deposit(amount)
      case AccountWithdraw(bankAccount, amount) => bankAccount ! Withdraw(amount)
      case AccountStatement(bankAccount) => bankAccount ! Statement
      case TransactionSuccess(message) => println(message)
      case TransactionFailure(message) => println(message)
      case amount: Int => println(s"this is the amount on my bank account: $amount")
    }
  }
  val bankAccount = system.actorOf(Props[BankAccountActor], "bankAccount")
  val banker = system.actorOf(Props[BankCustomer], "bankCustomer")

  import BankAccountActor._

  banker ! AccountDeposit(bankAccount, 5000)
  banker ! AccountDeposit(bankAccount, 5000)
  banker ! AccountWithdraw(bankAccount, 999999)
  banker ! AccountWithdraw(bankAccount, 8000)
  banker ! AccountStatement(bankAccount)
}
