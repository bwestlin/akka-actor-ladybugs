package ladybugs.http

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import ladybugs.calculation.Vec2d
import ladybugs.entities.{LadybugArena, Position}
import ladybugs.json.JsonProtocol._
import spray.can.websocket.frame.TextFrame
import spray.json._


object WebsocketConnection {
  def props() = Props(classOf[WebsocketConnection])

  sealed trait Command
  case class NoCommand(debug: String) extends Command
  case class Spawn(position: Seq[Int]) extends Command
}

class WebsocketConnection extends Actor with ActorLogging {

  import ladybugs.http.WebsocketConnection._

  context.system.eventStream.publish(LadybugArena.ArenaParticipationRequest(self))

  def receive = default(ActorRef.noSender)

  def default(arenaRef: ActorRef): Receive = {

    case LadybugArena.ArenaParticipationResponse() =>
      context.become(default(sender()))

    case TextFrame(bs) =>
      val json = bs.utf8String.parseJson

      val jsObj = json.asJsObject

      val commands = jsObj.fields.toSeq.map { case (name, jsValue) => parseCommand(name, jsValue) }

      commands.foreach {
        case Spawn(position) =>
          arenaRef ! LadybugArena.Spawn(Some(Position(Vec2d(position(0), position(1)))), Some(100))
        case NoCommand(debug) =>
          log.info(s"Unknown command: $debug")
      }
  }

  def parseCommand(name: String, jsValue: JsValue): Command = name match {
    case "spawn" => jsValue.convertTo[Spawn]
    case _ => NoCommand(s"$name: $jsValue")
  }
}
