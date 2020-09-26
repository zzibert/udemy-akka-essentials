package Part2Actors

import akka.actor.{Actor, ActorPath, Props, ActorSystem}

object ChildActorsExercise extends App {

  // Distributed word counting

  val system = ActorSystem("WordCountSystem")

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCounterMaster extends Actor {
    import WordCounterMaster._

    override def receive: Receive = unInitialized

    def unInitialized: Receive = {
      case Initialize(nChildren) => {
        (0 to nChildren).foreach(n => {
          context.actorOf(Props[WordCounterWorker], s"WordCountWorker-$n")
        })
        context.become(initialized(0, nChildren, 1))
      }
    }

    def initialized(currentWorker: Int, nChildren: Int, currentTaskId: Int): Receive = {
      case text: String => {
        val workerRef = system.actorSelection(s"/user/WordCountMaster/WordCountWorker-$currentWorker")
        workerRef ! WordCountTask(currentTaskId, text)
        val nextWorker = (currentWorker+1) % nChildren
        context.become(initialized(nextWorker, nChildren, currentTaskId+1))
      }
      case WordCountReply(id, count) => {
        println(s"worker: ${sender.path} responded with count $count for task: $id")
      }
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(currentTaskId, text) => {
        val numberOfWords = text.split(" ").length
        sender ! WordCountReply(currentTaskId, numberOfWords)
      }
    }
  }

  // create WordCountMaster
  // send Initialize(10) to WordCountMaster
  // send "akka is awesome" to WordCounterMaster
  // wcm will send a WordCountTask("...") to one of its children
  // child replies with a WordCountReply to WCM
  // Master replies to sender with the number 3
  // requestor -> wcm -> wcw
  // requestor <- wcm <- wcw
  // round robin logic
  // 1, 2, 3, 4, ..., n, 1, 2, 3, 4,

  import WordCounterMaster._
  val wordCounterMaster = system.actorOf(Props[WordCounterMaster], "WordCountMaster")

  wordCounterMaster ! Initialize(30000)

  (1 to 234200).foreach(id => {
    wordCounterMaster ! "ena"
    wordCounterMaster ! "ena dva"
    wordCounterMaster ! "ena dva tri"
    wordCounterMaster ! "ena dva tri stiri"
    wordCounterMaster ! "ena dva tri stiri pet"
    wordCounterMaster ! "ena dva tri stiri pet sest"
    wordCounterMaster ! "ena dva tri stiri pet sest sedem"
    wordCounterMaster ! "ena dva tri stiri pet sest sedem osem"
  })





}
