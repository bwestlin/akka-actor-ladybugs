package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Ladybug {
  case class Movement(self: ActorRef, x: Int, y: Int)

  def props(x: Int, y: Int) = Props(classOf[Ladybug], x, y)

}

class Ladybug(var x: Int, var y: Int) extends Actor with ActorLogging {

  import Ladybug._
  import LadybugArena._

  def receive = {
    case TimeToMove() => {
      sender() ! MovementRequest(x + 1, y + 1)
    }
    case MovementRequestResponse(ok, request) if ok => {
      x = request.x
      y = request.y

      context.system.eventStream.publish(Movement(self, x, y))
    }
  }
}
