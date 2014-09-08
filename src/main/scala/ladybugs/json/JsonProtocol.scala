package ladybugs.json

import spray.json._
import ladybugs.entities.Ladybug.Movement
import akka.actor.ActorRef

object JsonProtocol extends DefaultJsonProtocol {
  implicit object ActorRefFormat extends RootJsonFormat[ActorRef] {
    override def read(json: JsValue): ActorRef = ???
    override def write(obj: ActorRef): JsValue = JsString(obj.toString())
  }
  implicit val movementFormat = jsonFormat4(Movement)
}
