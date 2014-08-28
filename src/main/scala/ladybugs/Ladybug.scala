package ladybugs

import akka.actor.{ActorLogging, Actor, Props}

object Ladybug {
  def props(x: Int, y: Int) = Props(classOf[Ladybug], x, y)

}

class Ladybug(var x: Int, var y: Int) extends Actor with ActorLogging {

  import LadybugArena._

  def receive = {
    case TimeToMove() => {
      sender() ! MovementRequest(x + 1, y + 1)
    }
    case MovementRequestResponse(ok, request) if ok => {
      x = request.x
      y = request.y
    }
  }
}
