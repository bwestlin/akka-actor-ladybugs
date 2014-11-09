package ladybugs.json

import ladybugs.calculation.Vec2d
import ladybugs.entities.{LadybugPosition, Gender, LadybugState}
import ladybugs.entities.Ladybug.Movement
import akka.actor.ActorRef
import spray.json._

object JsonProtocol extends DefaultJsonProtocol {
  implicit object ActorRefFormat extends RootJsonFormat[ActorRef] {
    override def read(json: JsValue): ActorRef = ???
    override def write(obj: ActorRef): JsValue = JsString(obj.toString())
  }

  implicit object GenderFormat extends RootJsonFormat[Gender.Value] {
    override def read(json: JsValue): Gender.Value = ???
    override def write(gender: Gender.Value): JsValue = JsString(gender.toString)
  }

  implicit val vec2dFormat = jsonFormat2(Vec2d.apply)

  implicit val ladybugPositionFormat = jsonFormat2(LadybugPosition)

  implicit object LadybugStateFormat extends RootJsonFormat[LadybugState] {
    val baseFormat =  jsonFormat9(LadybugState)
    override def read(json: JsValue): LadybugState = ???
    override def write(state: LadybugState): JsValue = baseFormat.write(state) match {
      case JsObject(fields) => JsObject(fields + ("stage" -> JsString(state.stage.toString)))
    }
  }

  implicit val movementFormat = jsonFormat3(Movement)
}
