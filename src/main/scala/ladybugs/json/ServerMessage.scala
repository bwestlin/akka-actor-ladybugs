package ladybugs.json

import spray.json._
import ladybugs.json.JsonProtocol._


case class ServerMessage(`type`: String, payload: JsValue)

object ServerMessage {
  def jsonPayload[T : JsonWriter](msgType: String, payload: T) = ServerMessage(msgType, payload.toJson).toJson
}