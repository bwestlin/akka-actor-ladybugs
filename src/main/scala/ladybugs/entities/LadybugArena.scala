package ladybugs.entities

import akka.actor._
import ladybugs.calculation.Vec2d
import ladybugs.entities.Ladybug.Movement

import scala.concurrent.duration._
import scala.util.Random

case class Position(pos: Vec2d, radius: Double = 20) {

  def distanceTo(otherPosition: Position): Double = {
    pos.distanceTo(otherPosition.pos)
  }
}

object Stone {
  val width, height = 40
}

case class Stone(pos: Vec2d)

object LadybugArena {

  def props(width: Int, height: Int) = Props(classOf[LadybugArena], width, height)

  final val MovementInterval = 100.milliseconds
  final val SpawnerInterval = 5.second

  sealed trait Request
  sealed trait Response
  sealed trait Subscribes
  sealed trait Publishes

  case class Spawn(maybePosition: Option[Position] = None, maybeAge: Option[Int] = None) extends Request
  case class Kill(ladybugId: String) extends Request
  case class InitiateMovement() extends Request
  case class MovementRequest(direction: Vec2d, radius: Double) extends Request
  case class MovementRequestResponse(ok: Boolean, request: MovementRequest, position: Position, nearbyLadybugs: Seq[ActorRef])

  case class ArenaUpdates(movements: Set[Movement], stones: Set[Stone], numParticipants: Int) extends Publishes
  case class ArenaParticipationRequest(participator: ActorRef) extends Subscribes
  case class ArenaParticipationResponse() extends Response

  case class PutStone(x: Int, y: Int) extends Request
  case class RemoveStone(x: Int, y: Int) extends Request

  case class SpawnIfLessThan(num: Int) extends Request

  private case class LadybugArenaState(ladybugs: Map[ActorRef, Position],
                                       stones: Set[Stone],
                                       spawnCounter: Int,
                                       awaitingMovementsFrom: Set[ActorRef],
                                       movements: Set[Movement],
                                       participants: Set[ActorRef])
}

class LadybugArena(val width: Int, val height: Int) extends Actor with ActorLogging {
  import ladybugs.entities.LadybugArena._
  import context.dispatcher

  val mover = context.system.scheduler.schedule(MovementInterval, MovementInterval, self, InitiateMovement())
  val spawner = context.system.scheduler.schedule(SpawnerInterval, SpawnerInterval, self, SpawnIfLessThan(10))

  context.system.eventStream.subscribe(self, classOf[ArenaParticipationRequest])

  override def postStop(): Unit = {
    super.postStop()
    mover.cancel()
    spawner.cancel()
    context.system.eventStream.unsubscribe(self)
  }

  def movementWithinBounds(reqP: Position, currP: Position): Boolean =
    (reqP, currP) match {
      case (Position(Vec2d(rX, _), r), Position(Vec2d(cX, _), _)) if rX - r < 0       && rX < cX => false
      case (Position(Vec2d(rX, _), r), Position(Vec2d(cX, _), _)) if rX + r >= width  && rX > cX => false
      case (Position(Vec2d(_, rY), r), Position(Vec2d(_, cY), _)) if rY - r < 0       && rY < cY => false
      case (Position(Vec2d(_, rY), r), Position(Vec2d(_, cY), _)) if rY + r >= height && rY > cY => false
      case _ => true
    }

  def adjustPositionWithinBounds(p: Position) = {
    val x =
      if (p.pos.x - p.radius < 0) p.radius
      else if (p.pos.x + p.radius >= width) width - p.radius
      else p.pos.x

    val y =
      if (p.pos.y - p.radius < 0) p.radius
      else if (p.pos.y + p.radius >= height) height - p.radius
      else p.pos.y

    p.copy(Vec2d(x, y), p.radius)
  }

  def positionBlocked(requestedPosition: Position,
                      previousPosition: Position,
                      ladybugs: Map[ActorRef, Position],
                      stones: Set[Stone]): Boolean = {

    val anyLadybugBlocking =
      ladybugs.exists { case (_, position) =>
        val requestPositionDistance = requestedPosition.distanceTo(position)
        val previousPositionDistance = previousPosition.distanceTo(position)

        requestPositionDistance - requestedPosition.radius - position.radius < 0 && requestPositionDistance < previousPositionDistance
      }

    val stoneHalfW = Stone.width / 2
    val stoneHalfH = Stone.height / 2

    def stoneDistanceSquared(stone: Stone, pos: Vec2d) = {
      val closestX =
        if (pos.x < stone.pos.x - stoneHalfW) stone.pos.x - stoneHalfW
        else if (pos.x > stone.pos.x + stoneHalfW) stone.pos.x + stoneHalfW
        else pos.x
      val closestY =
        if (pos.y < stone.pos.y - stoneHalfH) stone.pos.y - stoneHalfH
        else if (pos.y > stone.pos.y + stoneHalfH) stone.pos.y + stoneHalfH
        else pos.y

      val distanceX = pos.x - closestX
      val distanceY = pos.y - closestY

      (distanceX * distanceX) + (distanceY * distanceY)
    }

    val anyStoneBlocking =
      stones.exists { stone =>
        val requestedDistanceSquared = stoneDistanceSquared(stone, requestedPosition.pos)
        val previousDistanceSquared = stoneDistanceSquared(stone, previousPosition.pos)

        requestedDistanceSquared < (requestedPosition.radius * requestedPosition.radius) &&
          requestedDistanceSquared < previousDistanceSquared
      }

    anyLadybugBlocking || anyStoneBlocking
  }

  def adjustPositionIfOverlapped(requestedPosition: Position, ladybugs: Map[ActorRef, Position]) = {
    val ladybugPosition = ladybugs.values.toSeq

    def tryPosition(position: Position): Stream[Position] = {
      val closesOverlapping = ladybugPosition.filter { otherPosition =>
            position.distanceTo(otherPosition) - position.radius - otherPosition.radius < 0
        }.sortBy(position.distanceTo).reverse.headOption

      closesOverlapping.map { overlappingPosition =>
        val positionDiff = overlappingPosition.pos - position.pos
        val positionDiffNormalized =
          if (positionDiff.magnitude == 0) Vec2d.right.normalised
          else positionDiff.normalised

        val newPosition = position.copy(
          pos = position.pos + (positionDiffNormalized * (overlappingPosition.radius + position.radius))
        )
        newPosition #:: tryPosition(newPosition)
      }.getOrElse(position #:: Stream.empty)
    }

    tryPosition(requestedPosition).take(10).last
  }

  def nearbyLadybugs(position: Position, ladybugs: Map[ActorRef, Position]): Seq[ActorRef] = {
    ladybugs.filter { case (_, otherPosition) =>
      position.distanceTo(otherPosition) - position.radius - otherPosition.radius <= 4
    }.keys.toSeq
  }

  def stonePos(x: Int, y: Int): (Int, Int) = {
    (x - (x % Stone.width) + Stone.width / 2, y - (y % Stone.height) + Stone.height / 2)
  }

  private def advanceState(state: LadybugArenaState): LadybugArenaState = {
    context.become(default(state))
    state
  }

  def receive = default(LadybugArenaState(Map.empty, Set.empty, 0, Set.empty, Set.empty, Set.empty))

  def default(state: LadybugArenaState): Receive = {

    case InitiateMovement() =>
      state.ladybugs.keys.foreach(_ ! Ladybug.LetsMove())
      advanceState(state.copy(
        awaitingMovementsFrom = state.ladybugs.keys.toSet,
        movements = Set.empty
      ))
      if (state.awaitingMovementsFrom.nonEmpty || state.movements.nonEmpty)
        log.info("Dropping nonhandled movements because new movement round begun")

    case request @ MovementRequest(direction, radius) =>
      state.ladybugs.get(sender()).foreach { position =>
        val requestedPosition = Position(
          position.pos + direction,
          radius
        )

        val otherLadybugs = state.ladybugs - sender()
        val ok =
          movementWithinBounds(requestedPosition, position) &&
          !positionBlocked(requestedPosition, position, otherLadybugs, state.stones)

        val nextPosition =
          if (ok) requestedPosition
          else position

        if (position != nextPosition) {
          advanceState(state.copy(
            ladybugs = state.ladybugs.updated(sender(), nextPosition)
          ))
        }

        sender() ! MovementRequestResponse(ok, request, nextPosition, nearbyLadybugs(nextPosition, otherLadybugs))
      }

    case movement @ Movement(_, _, _) =>
      val restAwaitingMovementsFrom = state.awaitingMovementsFrom - sender()

      if (restAwaitingMovementsFrom.isEmpty) {
        context.system.eventStream.publish(
          ArenaUpdates(state.movements + movement, state.stones, state.participants.size)
        )
        advanceState(state.copy(
          awaitingMovementsFrom = restAwaitingMovementsFrom,
          movements = Set.empty
        ))
      }
      else {
        advanceState(state.copy(
          awaitingMovementsFrom = restAwaitingMovementsFrom,
          movements = state.movements + movement
        ))
      }

    case Spawn(maybePosition, maybeAge) =>
      if (state.ladybugs.size < 100) {
        val position = maybePosition.getOrElse(Position(Vec2d(Random.nextInt(width), Random.nextInt(height))))

        val adjustedPosition = adjustPositionWithinBounds(adjustPositionIfOverlapped(position, state.ladybugs))

        val ladybugId = s"ladybug${state.spawnCounter}"
        val ladybug = context.actorOf(Ladybug.props(ladybugId, maybeAge), ladybugId)

        context.watch(ladybug)
        advanceState(state.copy(
          ladybugs = state.ladybugs + (ladybug -> adjustedPosition),
          spawnCounter = state.spawnCounter + 1
        ))
      }

    case Kill(ladybugId) if ladybugId.startsWith("ladybug") =>
      context.child(ladybugId).foreach(_ ! Ladybug.Annihilate())

    case Terminated(ladybug) if state.ladybugs.contains(ladybug) =>
      advanceState(state.copy(
        ladybugs = state.ladybugs - ladybug,
        awaitingMovementsFrom = state.awaitingMovementsFrom - ladybug
      ))

    case ArenaParticipationRequest(participator) =>
      context.watch(participator)
      participator ! ArenaParticipationResponse()
      advanceState(state.copy(
        participants = state.participants + participator
      ))

    case Terminated(participator) if state.participants.contains(participator) =>
      advanceState(state.copy(
        participants = state.participants - participator
      ))

    case PutStone(x, y) =>
      val (stoneX, stoneY) = stonePos(x, y)
      if (stoneX > 0 && stoneX <= width && stoneY > 0 && stoneY <= height) {
        val stone = Stone(Vec2d(stoneX, stoneY))
        val nextState = advanceState(state.copy(stones = state.stones + stone))
      }

    case RemoveStone(x, y) =>
      val (stoneX, stoneY) = stonePos(x, y)
      val stone = Stone(Vec2d(stoneX, stoneY))
      advanceState(state.copy(stones = state.stones - stone))

    case SpawnIfLessThan(num) =>
      if (state.ladybugs.size < num) self ! Spawn()
  }
}
