package ladybugs.entities

import akka.actor._
import ladybugs.calculation.Vec2d
import ladybugs.entities.Ladybug.Movement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

case class LadybugPosition(pos: Vec2d, radius: Double = 20) {

  def distanceTo(otherPosition: LadybugPosition): Double = {
    pos.distanceTo(otherPosition.pos)
  }
}

object LadybugArena {

  def props(width: Int, height: Int) = Props(classOf[LadybugArena], width, height)

  val movementInterval = 100.milliseconds

  sealed trait Request
  sealed trait Response

  case class Spawn(maybePosition: Option[LadybugPosition] = None, maybeAge: Option[Int] = None) extends Request
  case class InitiateMovement() extends Request
  case class MovementRequest(direction: Vec2d, radius: Double) extends Request
  case class MovementRequestResponse(ok: Boolean, request: MovementRequest, position: LadybugPosition, nearbyLadybugs: Seq[ActorRef])
  case class ArenaUpdates(movements: Set[Movement]) extends Response
}

class LadybugArena(val width: Int, val height: Int) extends Actor with ActorLogging {

  import ladybugs.entities.LadybugArena._

  val mover = context.system.scheduler.schedule(movementInterval, movementInterval, self, InitiateMovement())

  override def postStop(): Unit = {
    super.postStop()
    mover.cancel()
  }

  def positionWithinBounds(p: LadybugPosition): Boolean = {
    p.pos.x >= p.radius && p.pos.y >= p.radius && p.pos.x < width - p.radius && p.pos.y < height - p.radius
  }

  def adjustPositionWithinBounds(p: LadybugPosition) = {
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

  def positionBlocked(requestedPosition: LadybugPosition,
                      previousPosition: LadybugPosition,
                      ladybugs: Map[ActorRef, LadybugPosition]): Boolean = {

    ladybugs.exists { case (_, position) =>
      val requestPositionDistance = requestedPosition.distanceTo(position)
      val previousPositionDistance = previousPosition.distanceTo(position)

      requestPositionDistance - requestedPosition.radius - position.radius < 0 && requestPositionDistance < previousPositionDistance
    }
  }

  def adjustPositionIfOverlapped(requestedPosition: LadybugPosition, ladybugs: Map[ActorRef, LadybugPosition]) = {
    val ladybugPosition = ladybugs.values.toSeq

    def tryPosition(position: LadybugPosition): Stream[LadybugPosition] = {
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

  def nearbyLadybugs(position: LadybugPosition, ladybugs: Map[ActorRef, LadybugPosition]): Seq[ActorRef] = {
    ladybugs.filter { case (_, otherPosition) =>
      position.distanceTo(otherPosition) - position.radius - otherPosition.radius <= 4
    }.keys.toSeq
  }

  def receive = default(Map.empty, 0, Set.empty, Set.empty)

  def default(ladybugs: Map[ActorRef, LadybugPosition],
              spawnCounter: Int,
              awaitingMovementsFrom: Set[ActorRef],
              movements: Set[Movement]): Receive = {

    case InitiateMovement() =>
      ladybugs.keys.foreach(_ ! Ladybug.LetsMove())
      context.become(default(ladybugs, spawnCounter, ladybugs.keys.toSet, Set.empty))
      if (awaitingMovementsFrom.nonEmpty || movements.nonEmpty)
        log.info("Dropping nonhandled movements because new movement round begun")

    case request @ MovementRequest(direction, radius) =>
      ladybugs.get(sender()).foreach { position =>
        val requestedPosition = LadybugPosition(
          position.pos + direction,
          radius
        )

        val otherLadybugs = ladybugs - sender()
        val ok = positionWithinBounds(requestedPosition) && !positionBlocked(requestedPosition, position, otherLadybugs)

        val nextPosition =
          if (ok) requestedPosition
          else adjustPositionWithinBounds(position)

        if (position != nextPosition)
          context.become(this.default(ladybugs.updated(sender(), nextPosition), spawnCounter, awaitingMovementsFrom, movements))

        sender() ! MovementRequestResponse(ok, request, nextPosition, nearbyLadybugs(nextPosition, otherLadybugs))
      }

    case movement @ Movement(_, _, _) =>
      val restAwaitingMovementsFrom = awaitingMovementsFrom - sender()

      if (restAwaitingMovementsFrom.isEmpty) {
        context.system.eventStream.publish(ArenaUpdates(movements + movement))
        context.become(default(ladybugs, spawnCounter, restAwaitingMovementsFrom, Set.empty))
      }
      else {
        context.become(default(ladybugs, spawnCounter, restAwaitingMovementsFrom, movements + movement))
      }

    case Spawn(maybePosition, maybeAge) =>
      if (ladybugs.size < 100) {
        val position = maybePosition.getOrElse(LadybugPosition(Vec2d(Random.nextInt(width), Random.nextInt(height))))

        val adjustedPosition = adjustPositionWithinBounds(adjustPositionIfOverlapped(position, ladybugs))

        val ladybugId = s"ladybug$spawnCounter"
        val ladybug = context.actorOf(Ladybug.props(ladybugId, maybeAge), ladybugId)

        context.watch(ladybug)
        context.become(this.default(ladybugs + (ladybug -> adjustedPosition), spawnCounter + 1, awaitingMovementsFrom, movements))
      }

    case Terminated(ladybug) =>
      context.become(this.default(ladybugs - ladybug, spawnCounter, awaitingMovementsFrom - ladybug, movements))
  }
}
