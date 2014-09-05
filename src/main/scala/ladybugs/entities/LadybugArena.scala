package ladybugs.entities

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object LadybugArena {
  def props(width: Int, height: Int, ladybugs: Seq[ActorRef]) = Props(classOf[LadybugArena], width, height, ladybugs)

  case class Spawn(x: Int, y: Int)
  case class InitiateMovement()
  case class TimeToMove()
  case class MovementRequest(x: Int, y: Int)
  case class MovementRequestResponse(ok: Boolean, request: MovementRequest)
}

class LadybugArena(val width: Int, val height: Int, val ladybugs: Seq[ActorRef]) extends Actor with ActorLogging {

  import LadybugArena._

  val mover = context.system.scheduler.schedule(100 milliseconds, 100 milliseconds, self, InitiateMovement())

  override def postStop(): Unit = {
    super.postStop()
    mover.cancel()
  }

  def receive = {
    case InitiateMovement() => {
      ladybugs.foreach(_ ! TimeToMove())
    }
    case r @ MovementRequest(x, y) => {
      val ok = true //x >= 0 && y >= 0 && x < width && y < height
      sender() ! MovementRequestResponse(ok, r)
    }
  }
}
