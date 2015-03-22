package ladybugs.entities

import akka.actor._
import ladybugs.calculation.Vec2d
import ladybugs.entities.Gender.Gender
import ladybugs.entities.Stage.Stage

import scala.concurrent.duration._
import scala.util.Random

object Gender extends Enumeration {
  type Gender = Value
  val male, female = Value
  def random = if (Random.nextBoolean()) male else female
}

object Stage extends Enumeration {
  type Stage = Value
  val egg, child, adult, old, dead, annihilated = Value

  def fromAge(age: Int) = {
    val ageDuration = LadybugArena.movementInterval * age

    if (ageDuration < 10.seconds) egg
    else if (ageDuration < 50.seconds) child
    else if (ageDuration < 200.seconds) adult
    else if (ageDuration < 250.seconds) old
    else if (ageDuration < 260.seconds) dead
    else annihilated
  }
}

case class LadybugState(directionAngle: Double = Random.nextDouble() * 360,
                        turningAngle: Double = 0,
                        blocked: Boolean = false,
                        age: Int = 0,
                        gender: Gender = Gender.random,
                        fertilityPercent: Int = 100,
                        fertilityDirection: Int = -1,
                        birthTime: Int = 0,
                        eggs: Int = 0) {

  def stage = Stage.fromAge(age)

  def fertile = stage == Stage.adult && (gender == Gender.male || fertilityPercent >= 90)

  def pregnant = gender == Gender.female && birthTime >= 0 && eggs > 0

  def pregnancyPossible = gender == Gender.female && fertile && !pregnant

  def tryBecomePregnant(otherGender: Gender, otherStage: Stage) = {
    if (pregnancyPossible && gender != otherGender && otherStage == Stage.adult) {
      copy(birthTime = 200, eggs = Random.nextInt(3) + 1)
    }
    else this
  }

  def incrementAge = {
    val nextAge = age + 1
    val nextFertilityPercent =
      if (gender == Gender.female) fertilityPercent + fertilityDirection
      else fertilityPercent
    val nextFertilityDirection =
      if (nextFertilityPercent == 0 || nextFertilityPercent == 100) -fertilityDirection
      else fertilityDirection
    val nextBirthTime =
      if (birthTime > 0) birthTime - 1
      else birthTime

    copy(
      age = nextAge,
      fertilityPercent = nextFertilityPercent,
      fertilityDirection = nextFertilityDirection,
      birthTime = nextBirthTime
    )
  }
}

trait LadybugBehaviour {

  def calculateNextTurningAngle(turningAngle: Double): Double = {
    val maxAngle = 3
    val angleAdjustment = (Random.nextDouble() - 0.5) * 1
    val nextTurningAngle = (turningAngle - turningAngle / 50) + angleAdjustment
    Math.signum(nextTurningAngle) * Math.min(Math.abs(nextTurningAngle), maxAngle)
  }

  def calculateNextMovement(state: LadybugState): (LadybugState, Vec2d) = state.stage match {
    case Stage.egg | Stage.dead => (state, Vec2d.none)
    case stage => {
      val nextTurningAngle =
        if (!state.blocked) calculateNextTurningAngle(state.turningAngle)
        else state.turningAngle
      val nextDirectionAngle = state.directionAngle + nextTurningAngle

      val angleRadian = nextDirectionAngle * Math.PI / 180

      val nextDirection = Vec2d.right.rotate(angleRadian).normalised

      val speed = (stage, state.gender) match {
        case (Stage.child, _) => 2
        case (Stage.old, _) => 1
        case (_, Gender.female) if state.pregnant => 2
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

  def handleMovement(state: LadybugState, position: Position): LadybugState = {
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

  def potentiallyLayEgg(state: LadybugState, position: Position): LadybugState = {
    if (state.pregnant && state.birthTime == 0) {
      val angleRadian = state.directionAngle * Math.PI / 180
      val birthPosition = -Vec2d.right.rotate(angleRadian)
      for (_ <- 1 to state.eggs) {
        layEgg(position.copy(pos = position.pos + birthPosition))
      }
      state.copy(eggs = 0)
    }
    else state
  }

  def layEgg(position: Position): Unit
}

object Ladybug {

  def props(selfId: String, maybeAge: Option[Int]) = {
    val state = maybeAge.map(age => LadybugState(age = age)).getOrElse(LadybugState())
    Props(classOf[Ladybug], selfId, state)
  }

  sealed trait Request
  sealed trait Response

  case class LetsMove() extends Request

  case class ReproductionRequest() extends Request
  case class ReproductionResponse(gender: Gender, stage: Stage) extends Response

  case class Movement(self: String, position: Position, state: LadybugState) extends Response
}

class Ladybug(val selfId: String, val initialState: LadybugState) extends Actor with LadybugBehaviour with ActorLogging {

  import ladybugs.entities.Ladybug._
  import ladybugs.entities.LadybugArena._

  override def receive = default(initialState)

  def advanceState(state: LadybugState): LadybugState = {
    context.become(default(state))
    state
  }

  def default(state: LadybugState): Receive = {

    case LetsMove() =>
      if (state.stage == Stage.annihilated) {
        self ! PoisonPill
      }
      else {
        val (nextState, nextDirection) = calculateNextMovement(state.incrementAge)

        sender() ! MovementRequest(nextDirection, radius(state))
        advanceState(nextState)
      }

    case MovementRequestResponse(ok, request, position, nearbyLadybugs) =>
      handleNearbyLadybugs(state, nearbyLadybugs)

      val newState =
        if (ok) handleMovement(state, position)
        else handleBlocked(state)

      advanceState(potentiallyLayEgg(newState, position))
      context.parent ! Movement(selfId, position, newState)

    case ReproductionRequest() =>
      sender() ! ReproductionResponse(state.gender, state.stage)

    case ReproductionResponse(otherGender, otherStage) =>
      advanceState(state.tryBecomePregnant(otherGender, otherStage))

  }

  def handleNearbyLadybugs(state: LadybugState, nearbyLadybugs: Seq[ActorRef]) = {
    if (state.pregnancyPossible)
      nearbyLadybugs.foreach(_ ! ReproductionRequest())
  }

  override def layEgg(position: Position): Unit = {
    sender() ! Spawn(Some(position), Some(0))
  }
}
