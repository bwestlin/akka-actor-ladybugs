package ladybugs

import akka.actor.ActorSystem
import akka.io.IO
import ladybugs.calculation.Vec2d
import ladybugs.entities.{LadybugArena, Ladybug}
import ladybugs.http.{LadybygWebsocketUpdater, HttpServer}
import spray.can.Http
import spray.can.server.UHttp

import scala.util.Random

object LadybugsMain extends App {

  def doMain() {
    implicit val system = ActorSystem()
    import system.dispatcher

    val server = system.actorOf(HttpServer.props(), "http")

    val port = Option(System.getProperty("http.port")).map(_.toInt).getOrElse(8080)

    IO(UHttp) ! Http.Bind(server, "localhost", port)

    val arenaWidth = 800
    val arenaHeight = 600

    val ladybugs = for (i <- (0 to 10).toSeq) yield {
      val props = Ladybug.props(
        Random.nextInt(arenaWidth),
        Random.nextInt(arenaHeight),
        Vec2d(1, 0).rotate(Random.nextDouble() * Math.PI)
      )
      system.actorOf(props, s"ladybug$i")
    }
    println(s"ladybugs=$ladybugs")
    val arena = system.actorOf(LadybugArena.props(arenaWidth, arenaHeight, ladybugs), "arena")

    val updater = system.actorOf(LadybygWebsocketUpdater.props(), "updater")

    //readLine("Hit ENTER to exit ...\n")
    //system.shutdown()
    //system.awaitTermination()
  }

  // because otherwise we get an ambiguous implicit if doMain is inlined
  doMain()
}
