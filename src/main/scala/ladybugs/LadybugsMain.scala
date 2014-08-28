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

    //readLine("Hit ENTER to exit ...\n")
    //system.shutdown()
    //system.awaitTermination()
  }

  // because otherwise we get an ambiguous implicit if doMain is inlined
  doMain()
}
