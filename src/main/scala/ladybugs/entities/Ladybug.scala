package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ladybugs.calculation.Vec2d
import ladybugs.entities.Gender.Gender
import scala.util.Random

object Gender extends Enumeration {
  type Gender = Value
  val male, female = Value
  def random = if (Random.nextBoolean()) male else female
}

case class LadybugState(directionAngle: Double = Random.nextDouble() * 360,
                        turningAngle: Double = 0,
                        blocked: Boolean = false,
                        age: Double = 0,
                        gender: Gender = Gender.random)

object Ladybug {
  case class Movement(self: ActorRef, position: LadybugPosition, state: LadybugState)

  def props() = {
    val state = LadybugState()
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
    val nextTurningAngle = (turningAngle - turningAngle / 50) + angleAdjustment
    Math.signum(nextTurningAngle) * Math.min(Math.abs(nextTurningAngle), maxAngle)
  }

  def calculateNextMovement(state: LadybugState) = {
    val nextTurningAngle = if (!state.blocked) calculateNextTurningAngle(state.turningAngle) else state.turningAngle
    val nextDirectionAngle = state.directionAngle + nextTurningAngle

    val angleRadian = nextDirectionAngle * Math.PI / 180

    val nextDirection = Vec2d.right.rotate(angleRadian).normalised

    val speed = 3

    val nextState = state.copy(
      directionAngle = nextDirectionAngle,
      turningAngle = nextTurningAngle,
      age = state.age + 1
    )

    (nextState, nextDirection * speed)
  }

  def radius(state: LadybugState): Double = {
    15
  }

  def advanceState(state: LadybugState): LadybugState = {
    context.become(alive(state))
    state
  }

  def handleMovement(state: LadybugState, position: LadybugPosition): LadybugState = {
    state.copy(blocked = false)
  }

  def handleBlocked(state: LadybugState): LadybugState = {
    if (!state.blocked) {
      val newTurningAngle = (if (Random.nextBoolean()) 1 else -1) * 5d
      state.copy(turningAngle = newTurningAngle, blocked = true)
    }
    else {
      state
    }
  }

  def alive(state: LadybugState): Receive = {
    case LetsMove() => {
      val (nextState, nextDirection) = calculateNextMovement(state)

      sender() ! MovementRequest(nextDirection, radius(state))
      advanceState(nextState)
    }
    case MovementRequestResponse(ok, request, position) => {
      val newState =
        if (ok) handleMovement(state, position)
        else handleBlocked(state)

      advanceState(newState)

      context.system.eventStream.publish(Movement(self, position, newState))
    }
  }
}
