package ladybugs.http

import akka.actor.{Actor, ActorLogging, Props}
import ladybugs.entities.LadybugArena
import ladybugs.entities.LadybugArena.ArenaUpdates
import ladybugs.json.JsonProtocol._
import spray.json._

object WebsocketUpdater {
  def props() = Props(classOf[WebsocketUpdater])
}

class WebsocketUpdater extends Actor with ActorLogging {

  context.system.eventStream.subscribe(self, classOf[LadybugArena.ArenaUpdates])

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(self)
  }

  def receive = {
    case au: ArenaUpdates =>
      context.system.eventStream.publish(BroadcastWS(au.toJson.toString()))
  }
}
