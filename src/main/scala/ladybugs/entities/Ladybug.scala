package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ladybugs.calculation.Vec2d

import scala.util.Random

case class LadybugState(x: Double,
                        y: Double,
                        direction: Vec2d)

object Ladybug {
  case class Movement(self: ActorRef, x: Double, y: Double, angle: Double)

  def props(x: Double, y: Double, direction: Vec2d) = {
    val state = LadybugState(x, y, direction)
    Props(classOf[Ladybug], state)
  }

}

class Ladybug(var state: LadybugState) extends Actor with ActorLogging {

  import Ladybug._
  import LadybugArena._

  def receive = {
    case TimeToMove() => {
      val maxAngle = 8
      val angleRadian = (Random.nextDouble() - 0.5) * 2 * maxAngle * Math.PI / 180
      state = state.copy(direction = state.direction.rotate(angleRadian).normalised)
      val speed = 2

      sender() ! MovementRequest(
        state.x + state.direction.x * speed,
        state.y + state.direction.y * speed
      )
    }
    case MovementRequestResponse(ok, request) => {
      if (ok) {
        state = state.copy(x = request.x, y = request.y)
      }

      context.system.eventStream.publish(Movement(self, state.x, state.y, state.direction.angle * -180 / Math.PI))
    }
  }
}
