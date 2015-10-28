package ladybugs.http

import akka.actor._
import ladybugs.http.WSStats.RegisterConnection
import spray.can.websocket.FrameCommandFailed
import spray.can.websocket.frame.TextFrame
import spray.can.{Http, websocket}
import spray.http.HttpRequest
import spray.routing.HttpServiceActor

final case class PushWS(msg: String)
final case class BroadcastWS(msg: String)

object HttpServer {
  def props(wsStats: ActorRef) = Props(classOf[HttpServer], wsStats)
}

class HttpServer(wsStats: ActorRef) extends Actor with ActorLogging {

  def receive = {
    // when a new connection comes in we register a WebSocketConnection actor as the per connection handler
    case Http.Connected(remoteAddress, localAddress) =>
      val serverConnection = sender()
      val conn = context.actorOf(HttpWorker.props(serverConnection, wsStats))
      serverConnection ! Http.Register(conn)
  }
}

object HttpWorker {
  def props(serverConnection: ActorRef, wsConnectionCounter: ActorRef) =
    Props(classOf[HttpWorker], serverConnection, wsConnectionCounter)
}

class HttpWorker(val serverConnection: ActorRef, wsConnectionCounter: ActorRef)
  extends HttpServiceActor
  with websocket.WebSocketServerWorker {

  context.system.eventStream.subscribe(self, classOf[BroadcastWS])

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(self)
  }

  override def receive = handshaking orElse businessLogicNoWebSocket orElse closeLogic

  def businessLogic: Receive = {

    case websocket.UpgradedToWebSocket =>
      if (context.children.isEmpty) {
        val wsConnection = context.actorOf(WSConnection.props())
        wsConnectionCounter ! RegisterConnection(wsConnection)
      }

    case PushWS(msg) => send(TextFrame(msg))

    case BroadcastWS(msg) => send(TextFrame(msg))

    case tf: TextFrame =>
      context.children.foreach(_ ! tf)

    case x: FrameCommandFailed =>
      log.error("frame command failed", x)

    case x: HttpRequest =>
  }

  def businessLogicNoWebSocket: Receive = {
    implicit val refFactory: ActorRefFactory = context

    runRoute {
      pathPrefix("webjars") {
        get {
          getFromResourceDirectory("META-INF/resources/webjars")
        }
      } ~
      getFromResourceDirectory("webapp") ~
      path("") {
        getFromResource("webapp/index.html")
      }
    }
  }
}
