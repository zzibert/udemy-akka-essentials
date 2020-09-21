package part1Recap

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MultithreadingRecap extends App {
  // creating threads on the JVM

  val aThread = new Thread(() => println("im running in parallel"))
  aThread.start()
  aThread.join()

  val threadHello = new Thread(() => (1 to 1000).foreach(_ => println("hello")))
  val threadGoodbye = new Thread(() => (1 to 1000).foreach(_ => println("goodbye")))

  threadHello.start()
  threadGoodbye.start()

  // different runs produce different results!

  class BankAccount(@volatile private var amount: Int) {
    override def toString: String = "" + amount

    def withdraw(money: Int) = this.amount -= money

    def safeWithdraw(money: Int) = this.synchronized {
      this.amount -= money
    }
  }

  // inter-thread communication on the JVM
  // wait-notify

  // Scala Future
  val future = Future {
    // long computation - on a different thread
    42
  }

  future.onComplete {
    case Success(42) => println("I found the meaning of life")
    case Failure(_) => println("Domething happened with the meaning of life")
  }

  val aProcessedFuture = future.map(_ + 1) // future with 43

}
