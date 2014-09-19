package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object LadybugArena {
  def props(width: Int, height: Int, ladybugs: Seq[ActorRef]) = Props(classOf[LadybugArena], width, height, ladybugs)

  case class Spawn(x: Double, y: Double)
  case class InitiateMovement()
  case class LetsMove()
  case class MovementRequest(x: Double, y: Double)
  case class MovementRequestResponse(ok: Boolean, request: MovementRequest)
}

class LadybugArena(val width: Int, val height: Int, val ladybugs: Seq[ActorRef]) extends Actor with ActorLogging {

  import LadybugArena._

  val movementInterval = 100 milliseconds

  val mover = context.system.scheduler.schedule(movementInterval, movementInterval, self, InitiateMovement())

  override def postStop(): Unit = {
    super.postStop()
    mover.cancel()
  }

  def receive = {
    case InitiateMovement() => {
      ladybugs.foreach(_ ! LetsMove())
    }
    case r @ MovementRequest(x, y) => {
      val ok = x >= 0 && y >= 0 && x < width && y < height
      sender() ! MovementRequestResponse(ok, r)
    }
  }
}
