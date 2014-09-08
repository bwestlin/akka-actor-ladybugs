package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ladybugs.calculation.Vec2d

import scala.util.Random

object Ladybug {
  case class Movement(self: ActorRef, x: Double, y: Double, angle: Double)

  def props(x: Double, y: Double, direction: Vec2d = Vec2d(1, 0)) = Props(classOf[Ladybug], x, y, direction)

}

class Ladybug(var x: Double,
              var y: Double,
              var direction: Vec2d) extends Actor with ActorLogging {

  import Ladybug._
  import LadybugArena._

  def receive = {
    case TimeToMove() => {
      val maxAngle = 8
      val angleRadian = (Random.nextInt(maxAngle * 2 + 1) - maxAngle) * Math.PI / 180
      direction = direction.rotate(angleRadian).normalised
      val speed = 2

      sender() ! MovementRequest(x + direction.x * speed, y + direction.y * speed)
    }
    case MovementRequestResponse(ok, request) if ok => {
      x = request.x
      y = request.y

      context.system.eventStream.publish(Movement(self, x, y, direction.angle * -180 / Math.PI))
    }
  }
}
