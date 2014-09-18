package ladybugs.json

import ladybugs.entities.{Gender, LadybugState}
import spray.json._
import ladybugs.entities.Ladybug.Movement
import akka.actor.ActorRef

object JsonProtocol extends DefaultJsonProtocol {
  implicit object ActorRefFormat extends RootJsonFormat[ActorRef] {
    override def read(json: JsValue): ActorRef = ???
    override def write(obj: ActorRef): JsValue = JsString(obj.toString())
  }

  implicit object GenderFormat extends RootJsonFormat[Gender.Value] {
    override def read(json: JsValue): Gender.Value = ???
    override def write(gender: Gender.Value): JsValue = JsString(gender.toString)
  }

  implicit val ladybugStateFormat = jsonFormat6(LadybugState)
  implicit val movementFormat = jsonFormat2(Movement)
}
