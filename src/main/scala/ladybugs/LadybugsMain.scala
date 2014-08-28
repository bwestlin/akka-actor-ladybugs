package ladybugs

import akka.actor.ActorSystem
import akka.io.IO
import spray.can.Http
import spray.can.server.UHttp

object LadybugsMain extends App {

  def doMain() {
    implicit val system = ActorSystem()
    import system.dispatcher

    val server = system.actorOf(HttpServer.props(), "http")

    IO(UHttp) ! Http.Bind(server, "localhost", 8080)

    val ladybugs = for (i <- (0 to 10).toSeq) yield {
      system.actorOf(Ladybug.props(i, i), s"ladybug$i")
    }
    val arena = system.actorOf(LadybugArena.props(100, 100, ladybugs), "arena")

    //readLine("Hit ENTER to exit ...\n")
    //system.shutdown()
    //system.awaitTermination()
  }

  // because otherwise we get an ambiguous implicit if doMain is inlined
  doMain()
}
