package part6Patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, duration}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "An authenticator" should {
    authenticatorTestSuite(Props[AuthManager])
  }

  "An piped Authenticator" should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }

  def authenticatorTestSuite(props: Props) = {
    import AuthManager._
    "fail to authentice a non-registered user" in {
      val authManager = system.actorOf(props)

      authManager ! Authenticate("username", "password")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authentice if invalid password" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("username", "password")

      authManager ! Authenticate("username", "trololo")

      expectMsg(AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
    }

    "succesfully authentice a registered user" in {
      val authManager = system.actorOf(props)
      authManager ! RegisterUser("username", "password")
      authManager ! Authenticate("username", "password")

      expectMsg(AuthSuccess)
    }
  }

}

object AskSpec {

  // this code is somewhere else
  case class Read(key: String)
  case class Write(key: String, value: String)
  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key $key")
        sender ! kv.get(key) // Option[String]

      case Write(key, value) =>
        log.info(s"Trying to write the value $value at the key $key")
        context.become(online(kv + ((key) -> value)))
    }
  }
  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password not correct"
    val AUTH_FAILURE_SYSTEM_ERROR = "system error"
  }

  import AuthManager._

  class AuthManager extends Actor with ActorLogging {

    import AuthManager._

    // step 2 - logistics
    implicit val timeout: Timeout = Timeout(3 second)
    implicit val executionContext: ExecutionContext = context.dispatcher

    protected val authDb = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) => authDb ! Write(username, password)
      case Authenticate(username, password) => handleAuthentication(username, password)
    }

    def handleAuthentication(username: String, password: String) = {
      val originalSender = sender
      // ask the actor

      // handle the future
      val future = authDb ? Read(username) // Future[Any]
      future.onComplete {
        // step 5 most important
        // NEVER CALL METHODS ON ACTOR INSTANCE OR ACCESS MUTABLE STATE IN ONCOMPLETE
        // Avoid closing over
        case Success(None) => originalSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(dbPassword) =>
          if (dbPassword == password) originalSender ! AuthSuccess
          else sender ! AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) => originalSender ! AuthFailure(AUTH_FAILURE_SYSTEM_ERROR)
      }
    }
  }

  class PipedAuthManager extends AuthManager {

    import AuthManager._

    override def handleAuthentication(username: String, password: String): Unit = {
      // step 3 ask the actor
      val future = authDb ? Read(username) // Future[Any]
      // step 4 - process the future until you get the response you will send back
      val passwordFuture = future.mapTo[Option[String]] // Future[Option[String]]
      val responseFuture = passwordFuture.map {
        case None => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password)  AuthSuccess
          else AuthFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
      }
      // step 5 - pipe the resulting future to the actor you want to send the result to
      // when the future completes send the response to the actor ref in the arg list
      responseFuture.pipeTo(sender)
    }
  }
}
