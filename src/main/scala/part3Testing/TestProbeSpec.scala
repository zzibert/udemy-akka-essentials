package part3Testing

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}

object TestProbeSpec {
  // word counting actor hierarchy master-slave

  // send some word to the master
  // master sends the slave the pice of work
  // slave processes the work and replies to master
  // master aggregates the result
  // master sends the total count to the original requester
  case class Register(slaveRef: ActorRef)
  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) => context.become(online(slaveRef, 0))
      case _ => // ignore
    }
  }

}
