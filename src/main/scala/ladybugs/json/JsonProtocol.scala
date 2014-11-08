package ladybugs.json

import ladybugs.entities.{LadybugPosition, Gender, LadybugState}
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

  implicit val ladybugPositionFormat = jsonFormat3(LadybugPosition)

  implicit object LadybugStateFormat extends RootJsonFormat[LadybugState] {
    val baseFormat =  jsonFormat9(LadybugState)
    override def read(json: JsValue): LadybugState = ???
    override def write(state: LadybugState): JsValue = baseFormat.write(state) match {
      case JsObject(fields) => JsObject(fields + ("stage" -> JsString(state.stage.toString)))
    }
  }

  implicit val movementFormat = jsonFormat3(Movement)
}
