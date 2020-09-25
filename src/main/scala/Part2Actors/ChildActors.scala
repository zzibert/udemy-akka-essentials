package Part2Actors

import Part2Actors.ChildActors.CreditCard.AttachToAccount
import Part2Actors.ChildActors.Parent.CreateChild
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App {

  // actors can create other actors
  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {
    import Parent._


    override def receive: Receive = {
      case CreateChild(name) => {
        println(s"${self.path} Creating child")
        // create a new actor inside receive handler
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
      }

    }
    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => childRef forward message
    }

  }

  class Child extends Actor {

    override def receive: Receive = {
      case message => println(s"${self.path} I got $message")
    }
  }

  val system = ActorSystem("ParentChildDemo")
  val parent = system.actorOf(Props[Parent], "parent")

  import Parent._

  parent ! CreateChild("child")


  parent ! TellChild("yolo")

  // actor hierarchies

  // Guardian actors (top-level)
  // /system -> system guardian
  // /user -> user-level guardian
  // / = the root guardian manages system and user guardians

  // Actor selection
  val childSelection = system.actorSelection("/user/parent/child")

  childSelection ! "i found you"

  // Danger
  // NEVER PASS MUTABLE ACTOR STATES OR THE 'THIS' REFERENCE TO CHILD ACTORS
  // THIS BREAKS ACTOR ENCAPSULATION
  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }

  class NaiveBankAccount extends Actor {
    import NaiveBankAccount._
    import CreditCard._

    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard], "card")
        creditCardRef ! AttachToAccount(this) // !!

      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)

    }
    def deposit(funds: Int) = {
      println(s"${self.path} depositing $funds on top of $amount")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} withdrawing $funds on top of $amount")
      amount -= funds
    }
  }

  object CreditCard {
    case class AttachToAccount(bankAccountRef: ActorRef) // !!
    case object CheckStatus
  }

  class CreditCard extends Actor {
    import CreditCard._
    override def receive: Receive = {
      case AttachToAccount(account) => context.become(attached(account))
    }

    def attached(account: NaiveBankAccount): Receive = {
      case CheckStatus => {
        println(s"${self.path} your message has been processed")
        account.withdraw(1) // NEVER CLOSE OVER !!!
      }
    }
  }
  import NaiveBankAccount._
  import CreditCard._

  val BankAccount = system.actorOf(Props[NaiveBankAccount], "account")
  BankAccount ! InitializeAccount

  BankAccount ! Deposit(100)

  Thread.sleep(500)

  val creditCard = system.actorSelection("/user/account/card")

  creditCard ! CheckStatus

}
