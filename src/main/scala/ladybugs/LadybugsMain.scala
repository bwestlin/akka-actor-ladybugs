package ladybugs

import akka.actor.ActorSystem
import akka.io.IO
import ladybugs.entities.LadybugArena.Spawn
import ladybugs.entities.LadybugArena
import ladybugs.http.{LadybugWebsocketUpdater, HttpServer}
import spray.can.Http
import spray.can.server.UHttp

import scala.util.Random

object LadybugsMain extends App {

  def doMain() {
    implicit val system = ActorSystem()

    val server = system.actorOf(HttpServer.props(), "http")

    val port = Option(System.getProperty("http.port")).map(_.toInt).getOrElse(8080)

    IO(UHttp) ! Http.Bind(server, "0.0.0.0", port)

    val arenaWidth = 800
    val arenaHeight = 600

    val arena = system.actorOf(LadybugArena.props(arenaWidth, arenaHeight), "arena")

    for (_ <- 1 to 20) arena ! Spawn(maybeAge = Some((50 + Random.nextInt(50)) * 10))

    val updater = system.actorOf(LadybugWebsocketUpdater.props(), "updater")
  }

  // because otherwise we get an ambiguous implicit if doMain is inlined
  doMain()
}
