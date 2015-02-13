package ladybugs.http

import akka.actor.{Props, ActorLogging, Actor}
import ladybugs.entities.Ladybug
import ladybugs.entities.Ladybug.Movement
import spray.json._
import ladybugs.json.JsonProtocol._

object LadybugWebsocketUpdater {
  def props() = Props(classOf[LadybugWebsocketUpdater])
}

class LadybugWebsocketUpdater extends Actor with ActorLogging {

  context.system.eventStream.subscribe(self, classOf[Ladybug.Movement])

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(self)
  }

  def receive = {
    case m: Movement => {
      context.system.eventStream.publish(BroadcastWS(m.toJson.toString()))
    }
  }
}
