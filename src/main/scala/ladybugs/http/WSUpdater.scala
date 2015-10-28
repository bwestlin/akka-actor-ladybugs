package ladybugs.http

import akka.actor.{Actor, ActorLogging, Props}
import ladybugs.entities.LadybugArena
import ladybugs.entities.LadybugArena.ArenaUpdates
import ladybugs.json.JsonProtocol._
import ladybugs.json.ServerMessage
import spray.json._

object WSUpdater {
  def props() = Props(classOf[WSUpdater])
}

class WSUpdater extends Actor with ActorLogging {

  context.system.eventStream.subscribe(self, classOf[LadybugArena.ArenaUpdates])
  context.system.eventStream.subscribe(self, classOf[WSStats.StatsUpdates])

  override def postStop(): Unit = {
    super.postStop()
    context.system.eventStream.unsubscribe(self)
  }

  def receive = {
    case au: ArenaUpdates =>
      publishUpdate("arena", au)
    case su: WSStats.StatsUpdates =>
      publishUpdate("stats", su)
  }

  def publishUpdate[T : JsonWriter](msgType: String, payload: T) = {
    context.system.eventStream.publish(
      BroadcastWS(ServerMessage.jsonPayload(msgType, payload).toString())
    )
  }
}
