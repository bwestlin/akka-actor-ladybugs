package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ladybugs.calculation.Vec2d

import scala.util.Random

case class LadybugState(x: Double,
                        y: Double,
                        directionAngle: Double = Random.nextDouble() * 360,
                        turningAngle: Double = 0,
                        age: Double = 0)

object Ladybug {
  case class Movement(self: ActorRef, state: LadybugState)

  def props(x: Double, y: Double) = {
    val state = LadybugState(x, y)
    Props(classOf[Ladybug], state)
  }

}

class Ladybug(val initialState: LadybugState) extends Actor with ActorLogging {

  import Ladybug._
  import LadybugArena._

  override def receive = alive(initialState)

  def calculateNextTurningAngle(turningAngle: Double) = {
    val maxAngle = 3
    val angleAdjustment = (Random.nextDouble() - 0.5) * 1
    val nextTurningAngle = turningAngle + angleAdjustment
    Math.signum(nextTurningAngle) * Math.min(Math.abs(nextTurningAngle), maxAngle)
  }

  def calculateNextMovement(state: LadybugState) = {
    val nextTurningAngle = calculateNextTurningAngle(state.turningAngle)
    val nextDirectionAngle = state.directionAngle + nextTurningAngle

    val angleRadian = nextDirectionAngle * Math.PI / 180

    val nextDirection = Vec2d.right.rotate(angleRadian).normalised

    val speed = 2
    val nextX = state.x + nextDirection.x * speed
    val nextY = state.y + nextDirection.y * speed


    val nextState = state.copy(
      directionAngle = nextDirectionAngle,
      turningAngle = nextTurningAngle,
      age = state.age + 1
    )

    (nextState, nextX, nextY)
  }

  def advanceState(state: LadybugState) = {
    context.become(alive(state))
  }

  def alive(state: LadybugState): Receive = {
    case TimeToMove() => {
      val (nextState, nextX, nextY) = calculateNextMovement(state)

      sender() ! MovementRequest(nextX, nextY)
      advanceState(nextState)
    }
    case MovementRequestResponse(ok, request) => {
      if (ok) {
        advanceState(state.copy(x = request.x, y = request.y))
      }

      context.system.eventStream.publish(Movement(self, state))
    }
  }
}
