package ladybugs

import akka.actor.ActorSystem
import akka.io.IO
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

    IO(UHttp) ! Http.Bind(server, "localhost", 8080)

    val ladybugs = for (i <- (0 to 10).toSeq) yield {
      system.actorOf(Ladybug.props(Random.nextInt(400), Random.nextInt(400)), s"ladybug$i")
    }
    println(s"ladybugs=$ladybugs")
    val arena = system.actorOf(LadybugArena.props(400, 400, ladybugs), "arena")

    val updater = system.actorOf(LadybygWebsocketUpdater.props(), "updater")

    //readLine("Hit ENTER to exit ...\n")
    //system.shutdown()
    //system.awaitTermination()
  }

  // because otherwise we get an ambiguous implicit if doMain is inlined
  doMain()
}
