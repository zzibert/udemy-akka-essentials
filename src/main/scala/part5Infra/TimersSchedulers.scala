package part5Infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

object TimersSchedulers extends App{

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo")

//  val simpleActor = system.actorOf(Props[SimpleActor])
//
//  system.log.info("Scheduling reminder for simpleActor")
//
//  import system.dispatcher
//
//  system.scheduler.scheduleOnce(3 second){
//    simpleActor ! "reminder"
//  }
//
//  val routine: Cancellable = system.scheduler.schedule(1 second, 2 second) {
//    simpleActor ! "heartbeat"
//  }
//
//  system.scheduler.scheduleOnce(5 seconds) {
//    routine.cancel()
//  }

  // 1 - implemt a self-closing actor
  // if an actor receives a message you have 1 second to send it another message
  // if the time window expires the actor will stop itself
  // if you send another message, the time window is reset

  case object Stop

  import system.dispatcher

  class SelfClosingActor extends Actor with ActorLogging {
    var schedule = createTimeoutWindow()
    def createTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! Stop
      }
    }

    override def receive: Receive = {
      case Stop =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Received $message, staying alive")
        schedule.cancel()
        schedule = createTimeoutWindow()
    }
  }

//  val selfClosingActor = system.actorOf(Props[SelfClosingActor])
//
//  val routine: Cancellable = system.scheduler.schedule(Duration.Zero, 500 millis) {
//    selfClosingActor ! "heartbeat"
//  }
//
//  val sleepingTime: Cancellable = system.scheduler.scheduleOnce(5 seconds) {
//    routine.cancel()
//  }
//
//  system.scheduler.scheduleOnce(10 second) {
//    system.log.info("sending pong to the self-closing actor")
//    selfClosingActor ! "pong"
//  }

  case object TimerKey

  case object Start
  case object Reminder

  // timer -> schedule message to yourself
  class TimerBasedHeartBeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)
    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("Im alive")
      case Stop =>
        log.warning("Stoping")
        timers.cancel(TimerKey)
        context.stop(self)
      case message => log.info(message.toString)
    }
  }

  val timerBasedHeartbeatActor = system.actorOf(Props[TimerBasedHeartBeatActor])

  system.scheduler.scheduleOnce(5 seconds) {
    timerBasedHeartbeatActor ! Stop
  }

  system.scheduler.scheduleOnce(6 seconds) {
    timerBasedHeartbeatActor ! "trololo"
  }

}
