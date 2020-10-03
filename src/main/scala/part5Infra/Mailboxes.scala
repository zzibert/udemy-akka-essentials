package part5Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

object Mailboxes extends App{

  val system = ActorSystem("MailboxDemo", ConfigFactory.load().getConfig("mailboxesDemo"))

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  // 1. A custom priority mailbox

  // step 1 - mailbox definition
  class SupportTicketPriorityMailbox(settings: ActorSystem.Settings, config: Config)
    extends UnboundedPriorityMailbox(PriorityGenerator {
      case message: String if message.startsWith("[P0]") => 0
      case message: String if message.startsWith("[P1]") => 1
      case message: String if message.startsWith("[P2]") => 2
      case message: String if message.startsWith("[P3]") => 3
      case _ => 4
    })

  // 2. step 2 - make it known in the config

  // 3. step - attach the dispatcher to an actor

  val supportTicketLogger = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))

  supportTicketLogger ! PoisonPill // Even PoisonPill is postponed

  Thread.sleep(1000) // after which time can I send another message be prioritized accordingly?

//  supportTicketLogger ! "[P3] this thing would be nice to have"
//
//  supportTicketLogger ! "[P0] this thing needs to be solved now"
//
//  supportTicketLogger ! "[P1] do this when you have time"

  // Interesting case nr. 2 - control aware mailbox
  // we will UnboundedControlAwareMailbox

  // step 1 - Mark important messages as control messages
  case object ManagementTicket extends ControlMessage

  // step 2 - Configure who gets the mailbox
  // make the actor attach to the mailbox
  val controlAwareActor = system.actorOf(Props[SimpleActor].withMailbox("control-mailbox"))

//  controlAwareActor ! "[P1] do this when you have time"
//  controlAwareActor ! "[P0] this thing needs to be solved now"
//
//  controlAwareActor ! ManagementTicket

  // method nr. 2 - using the deployment config

  val altControlAware = system.actorOf(Props[SimpleActor], "altControlAwareActor")

  altControlAware ! "[P1] do this when you have time"
  altControlAware ! "[P0] this thing needs to be solved now"

  altControlAware ! ManagementTicket



}
