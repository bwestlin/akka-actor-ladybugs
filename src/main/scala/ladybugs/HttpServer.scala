package ladybugs

import akka.actor._
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.{TextFrame, BinaryFrame}
import spray.can.{websocket, Http}
import spray.http.HttpRequest
import spray.routing.HttpServiceActor
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

final case class Push(msg: String)

object HttpServer {
  def props() = Props(classOf[HttpServer])
}

class HttpServer extends Actor with ActorLogging {
  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(HttpWorker.props(serverConnection))
      serverConnection ! Http.Register(conn)
  }
}

object HttpWorker {
  def props(serverConnection: ActorRef) = Props(classOf[HttpWorker], serverConnection)
}
class HttpWorker(val serverConnection: ActorRef) extends HttpServiceActor with websocket.WebSocketServerWorker {
  override def receive = handshaking orElse businessLogicNoWebSocket orElse closeLogic

  val pinger = context.system.scheduler.schedule(500 milliseconds, 500 milliseconds) {
    self ! Push("ping")
  }


  override def postStop(): Unit = {
    super.postStop()
    pinger.cancel()
  }

  def businessLogic: Receive = {
    // just bounce frames back for Autobahn testsuite
    case x @ (_: BinaryFrame | _: TextFrame) =>
      sender() ! x

    case Push(msg) => send(TextFrame(msg))

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case x: HttpRequest => // do something
  }

  def businessLogicNoWebSocket: Receive = {
    implicit val refFactory: ActorRefFactory = context

    runRoute {
      pathPrefix("webjars") {
        get {
          getFromResourceDirectory("META-INF/resources/webjars")
        }
      } ~
      getFromResourceDirectory("webapp")
    }
  }
}
