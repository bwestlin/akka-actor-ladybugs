package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ladybugs.calculation.Vec2d
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

  case class Spawn(maybePosition: Option[LadybugPosition] = None, maybeAge: Option[Int] = None)
  case class InitiateMovement()
  case class LetsMove()
  case class MovementRequest(direction: Vec2d, radius: Double)
  case class MovementRequestResponse(ok: Boolean, request: MovementRequest, position: LadybugPosition, nearbyLadybugs: Seq[ActorRef])
}

class LadybugArena(val width: Int, val height: Int) extends Actor with ActorLogging {

  import LadybugArena._

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

  def positionBlocked(requestedPosition: LadybugPosition, previousPosition: LadybugPosition, ladybugs: Map[ActorRef, LadybugPosition]): Boolean = {
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

  def receive = default(Map.empty)

  def default(ladybugs: Map[ActorRef, LadybugPosition]): Receive = {
    case InitiateMovement() => {
      ladybugs.keys.foreach(_ ! LetsMove())
    }
    case request @ MovementRequest(direction, radius) => {
      ladybugs.get(sender()).foreach { position =>
        val requestedPosition = LadybugPosition(
          position.pos + direction,
          radius
        )

        val otherLadybugs = ladybugs - sender()
        val ok = positionWithinBounds(requestedPosition) && !positionBlocked(requestedPosition, position, otherLadybugs)
        val nextPosition = if (ok) requestedPosition else adjustPositionWithinBounds(position)
        if (position != nextPosition) context.become(this.default(ladybugs.updated(sender(), nextPosition)))

        sender() ! MovementRequestResponse(ok, request, nextPosition, nearbyLadybugs(nextPosition, otherLadybugs))
      }
    }
    case Spawn(maybePosition, maybeAge) => {
      val position = maybePosition.getOrElse(LadybugPosition(Vec2d(Random.nextInt(width), Random.nextInt(height))))

      val adjustedPosition = adjustPositionWithinBounds(adjustPositionIfOverlapped(position, ladybugs))

      val ladybug = context.system.actorOf(Ladybug.props(maybeAge), s"ladybug${ladybugs.size}")

      context.become(this.default(ladybugs + (ladybug -> adjustedPosition)))
    }
  }
}
