package ladybugs

import akka.actor.{ActorRef, ActorLogging, Actor, Props}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object LadybugArena {
  def props(width: Int, height: Int, ladybugs: Seq[ActorRef]) = Props(classOf[LadybugArena], width, height, ladybugs)

  case class InitiateMovement()
  case class TimeToMove()
  case class MovementRequest(x: Int, y: Int)
  case class MovementRequestResponse(ok: Boolean, request: MovementRequest)
}

class LadybugArena(val width: Int, val height: Int, val ladybugs: Seq[ActorRef]) extends Actor with ActorLogging {

  import LadybugArena._

  val mover = context.system.scheduler.schedule(500 milliseconds, 500 milliseconds, self, InitiateMovement())

  override def postStop(): Unit = {
    super.postStop()
    mover.cancel()
  }

  def receive = {
    case InitiateMovement() => {
      log.info(s"MovementRequest()")
      ladybugs.foreach(_ ! TimeToMove())
    }
    case r @ MovementRequest(x, y) => {
      log.info(s"MovementRequest($x, $y)")
      val ok = x >= 0 && y >= 0 && x < width && y < height
      sender() ! MovementRequestResponse(ok, r)
    }
  }
}
