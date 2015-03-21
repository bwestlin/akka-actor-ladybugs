package ladybugs

import akka.actor.Actor
import akka.io.IO
import ladybugs.entities.LadybugArena
import ladybugs.entities.LadybugArena.Spawn
import ladybugs.http.{HttpServer, WebsocketUpdater}
import spray.can.Http
import spray.can.server.UHttp

import scala.util.Random


class ToplevelSupervisor extends Actor {

  implicit val system = context.system

  val server = context.actorOf(HttpServer.props(), "http")

  val port = Option(System.getProperty("http.port")).map(_.toInt).getOrElse(8080)

  IO(UHttp) ! Http.Bind(server, "0.0.0.0", port)

  val arenaWidth = 800
  val arenaHeight = 600

  val arena = context.actorOf(LadybugArena.props(arenaWidth, arenaHeight), "arena")

  for (_ <- 1 to 20) arena ! Spawn(maybeAge = Some((50 + Random.nextInt(50)) * 10))

  val updater = context.actorOf(WebsocketUpdater.props(), "updater")

  def receive = {
    case _ =>
  }
}
