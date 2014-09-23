package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ladybugs.calculation.Vec2d
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

case class LadybugPosition(x: Double, y: Double, radius: Double = 20) {
  def distanceTo(otherPosition: LadybugPosition): Double = {
    val a = if (x > otherPosition.x) x - otherPosition.x else otherPosition.x - x
    val b = if (y > otherPosition.y) y - otherPosition.y else otherPosition.y - y
    Math.sqrt(a * a + b * b)
  }
}

object LadybugArena {
  def props(width: Int, height: Int) = Props(classOf[LadybugArena], width, height)

  val movementInterval = 100 milliseconds

  case class Spawn(maybePosition: Option[LadybugPosition] = None, maybeAge: Option[Int] = None)
  case class InitiateMovement()
  case class LetsMove()
  case class MovementRequest(direction: Vec2d, radius: Double)
  case class MovementRequestResponse(ok: Boolean, request: MovementRequest, position: LadybugPosition)
}

class LadybugArena(val width: Int, val height: Int) extends Actor with ActorLogging {

  import LadybugArena._

  val mover = context.system.scheduler.schedule(movementInterval, movementInterval, self, InitiateMovement())

  override def postStop(): Unit = {
    super.postStop()
    mover.cancel()
  }

  def positionWithinBounds(p: LadybugPosition): Boolean = {
    p.x >= p.radius && p.y >= p.radius && p.x < width - p.radius && p.y < height - p.radius
  }

  def adjustPositionWithinBounds(p: LadybugPosition) = {
    val x =
      if (p.x - p.radius < 0) p.radius
      else if (p.x + p.radius >= width) width - p.radius
      else p.x

    val y =
      if (p.y - p.radius < 0) p.radius
      else if (p.y + p.radius >= height) height - p.radius
      else p.y

    p.copy(x, y, p.radius)
  }

  def positionBlocked(requestedPosition: LadybugPosition, ladybugs: Map[ActorRef, LadybugPosition]): Boolean = {
    ladybugs.exists { case (_, position) =>
        requestedPosition.distanceTo(position) - requestedPosition.radius - position.radius < 0
    }
  }

  def receive = default(Map.empty)

  def default(ladybugs: Map[ActorRef, LadybugPosition]): Receive = {
    case InitiateMovement() => {
      ladybugs.keys.foreach(_ ! LetsMove())
    }
    case request @ MovementRequest(direction, radius) => {
      ladybugs.get(sender()).foreach { position =>
        val requestedPosition = LadybugPosition(
          position.x + direction.x,
          position.y + direction.y,
          radius
        )

        val ok = positionWithinBounds(requestedPosition) && !positionBlocked(requestedPosition, ladybugs - sender())
        if (ok) context.become(this.default(ladybugs.updated(sender(), requestedPosition)))
        val nextPosition = if (ok) requestedPosition else position
        sender() ! MovementRequestResponse(ok, request, nextPosition)
      }
    }
    case Spawn(maybePosition, maybeAge) => {
      val position = maybePosition.getOrElse(LadybugPosition(
        Random.nextInt(width),
        Random.nextInt(height)
      ))

      val withinBoundsPosition = adjustPositionWithinBounds(position)

      val ladybug = context.system.actorOf(Ladybug.props(maybeAge), s"ladybug${ladybugs.size}")

      context.become(this.default(ladybugs + (ladybug -> withinBoundsPosition)))
    }
  }
}
