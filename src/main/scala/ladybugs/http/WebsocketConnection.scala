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
  case class Kill(id: String) extends Command
  case class PutStone(pos: Seq[Int]) extends Command
  case class RemoveStone(pos: Seq[Int]) extends Command
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
        case Kill(id) =>
          arenaRef ! LadybugArena.Kill(id)
        case PutStone(pos) =>
          arenaRef ! LadybugArena.PutStone(pos(0), pos(1))
        case RemoveStone(pos) =>
          arenaRef ! LadybugArena.RemoveStone(pos(0), pos(1))
        case NoCommand(debug) =>
          log.info(s"Unknown command: $debug")
      }
  }

  def parseCommand(name: String, jsValue: JsValue): Command = name match {
    case "spawn" => jsValue.convertTo[Spawn]
    case "kill" => jsValue.convertTo[Kill]
    case "putStone" => jsValue.convertTo[PutStone]
    case "removeStone" => jsValue.convertTo[RemoveStone]
    case _ => NoCommand(s"$name: $jsValue")
  }
}
