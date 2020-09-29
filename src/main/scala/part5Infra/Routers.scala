package part5Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router, Broadcast}
import com.typesafe.config.ConfigFactory

object Routers extends App {

  // 1 - manual router
  class Master extends Actor {
    // create routees
    private val slaves = for (number <- 1 to 5) yield {
      val slave = context.actorOf(Props[Slave], s"slave_$number")
      context.watch(slave)
      ActorRefRoutee(slave)
    }
    // step 2 - define router
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)
      case message => router.route(message, sender)
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"${self.path} - {$message.toString}")
    }
  }

  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))

  val master = system.actorOf(Props[Master])

//  for (i <- 1 to 10) {
//    master ! s" [$i] hello from the world"
//  }

  // METHOD NUMBER 2.1 - a router actor with its own childre
  // pool router

  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
//  for (i <- 1 to 10) {
//    poolMaster ! s" [$i] hello from the world"
//  }

  // METHOD 2.2 from configuration
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")

//  for (i <- 1 to 10) {
//    poolMaster2 ! s" [$i] hello from the world"
//  }

  // METHOD 3 - router with actors created elsewhere
  // GROUP ROUTER

  val slaveList = (1 to 5).map(i => system.actorOf(Props[Slave], s"slave_$i")).toList

  // need their paths
  val slavePaths = slaveList.map(slaveRef => slaveRef.path.toString)

  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())

//  for (i <- 1 to 10) {
//    groupMaster ! s" [$i] hello from the world"
//  }

  val groupMaster2 = system.actorOf(FromConfig.props(), "groupMaster2")

//  for (i <- 1 to 10) {
//    groupMaster2 ! s" [$i] hello from the world"
//  }

  // SPECIAL MESSAGES
  groupMaster2 ! Broadcast("Hello from Everyone")

  // POISON PILL AND KILL are not routed
  // AddRoutee, RemoveRoute, GetRoutee, get only handled by the routing actor
}
