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

object Stage extends Enumeration {
  type Stage = Value
  val egg, child, adult, old, dead = Value

  def fromAge(age: Int) = {
    import scala.concurrent.duration._
    val ageDuration = LadybugArena.movementInterval * age

    if (ageDuration < 20.seconds) egg
    else if (ageDuration < 50.seconds) child
    else if (ageDuration < 170.seconds) adult
    else if (ageDuration < 200.seconds) old
    else dead
  }
}

case class LadybugState(directionAngle: Double = Random.nextDouble() * 360,
                        turningAngle: Double = 0,
                        blocked: Boolean = false,
                        age: Int = 0,
                        gender: Gender = Gender.random) {

  def stage = Stage.fromAge(age)
}

object Ladybug {

  case class Movement(self: ActorRef, position: LadybugPosition, state: LadybugState)

  def props(maybeAge: Option[Int]) = {
    val state = maybeAge.map(age => LadybugState(age = age)).getOrElse(LadybugState())
    Props(classOf[Ladybug], state)
  }
}

class Ladybug(val initialState: LadybugState) extends Actor with ActorLogging {

  import Ladybug._
  import LadybugArena._

  override def receive = default(initialState)

  def calculateNextTurningAngle(turningAngle: Double) = {
    val maxAngle = 3
    val angleAdjustment = (Random.nextDouble() - 0.5) * 1
    val nextTurningAngle = (turningAngle - turningAngle / 50) + angleAdjustment
    Math.signum(nextTurningAngle) * Math.min(Math.abs(nextTurningAngle), maxAngle)
  }

  def calculateNextMovement(state: LadybugState) = state.stage match {
    case Stage.egg | Stage.dead => (state, Vec2d.none)
    case stage => {
      val nextTurningAngle =
        if (!state.blocked) calculateNextTurningAngle(state.turningAngle)
        else state.turningAngle
      val nextDirectionAngle = state.directionAngle + nextTurningAngle

      val angleRadian = nextDirectionAngle * Math.PI / 180

      val nextDirection = Vec2d.right.rotate(angleRadian).normalised

      val speed = stage match {
        case Stage.child => 2
        case Stage.old => 1
        case _ => 3
      }

      val nextState = state.copy(
        directionAngle = nextDirectionAngle,
        turningAngle = nextTurningAngle
      )

      (nextState, nextDirection * speed)
    }
  }

  def radius(state: LadybugState): Double = {
    state.stage match {
      case Stage.egg => 8
      case Stage.child => 10
      case _ => 15
    }
  }

  def advanceState(state: LadybugState): LadybugState = {
    context.become(default(state))
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

  def default(state: LadybugState): Receive = {
    case LetsMove() => {
      val (nextState, nextDirection) = calculateNextMovement(state.copy(age = state.age + 1))

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
