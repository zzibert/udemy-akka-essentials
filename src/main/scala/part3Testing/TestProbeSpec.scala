package part3Testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._

  "A master actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("actor")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
    }
    "a master actor should send the work to the slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"
      master ! Work(workloadString) // Implicit sender - test actor

      // the interaction between the master and the slave actor
      slave.expectMsg(SlaveWork(workloadString, testActor))
      slave.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3))
    }
    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")

      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workLoadString = "I love Akka"
      master ! Work(workLoadString)
      master ! Work(workLoadString)

      slave.receiveWhile() {
        case SlaveWork(`workLoadString`, `testActor`) => slave.reply(WorkCompleted(3, testActor))
      }
      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }
}

object TestProbeSpec {
  // word counting actor hierarchy master-slave

  // send some word to the master
  // master sends the slave the piece of work
  // slave processes the work and replies to master
  // master aggregates the result
  // master sends the total count to the original requester
  case class Register(slaveRef: ActorRef)
  case class SlaveWork(text: String, originalRequestor: ActorRef)
  case class Work(text: String)
  case class WorkCompleted(count: Int, originalRequestor: ActorRef)
  case class Report(totalCount: Int)
  case object RegistrationAck

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender ! RegistrationAck
        context.become(online(slaveRef, 0))
      case _ => // ignore
    }
    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender)
      case WorkCompleted(count, originalRequestor) =>
        val newTotalWordCount = totalWordCount + count
        originalRequestor ! Report(newTotalWordCount)
        context.become(online(slaveRef, newTotalWordCount))
    }
  }

  // class Slave...



}
