package ladybugs.json

import ladybugs.entities.Ladybug.Movement
import ladybugs.entities.LadybugArena.ArenaUpdates
import ladybugs.entities.Stone
import ladybugs.http.WebsocketConnection
import spray.json._

object JsonProtocol extends DefaultJsonProtocol {

  implicit object MovementFormat extends RootJsonFormat[Movement] {
    override def read(json: JsValue): Movement = ???
    override def write(movement: Movement): JsValue = {
      JsObject(Map(
        "id"      -> JsString(movement.self),
        "pos"     -> JsArray(Vector(movement.position.pos.x, movement.position.pos.y).map(_.toInt).map(JsNumber.apply)),
        "stage"   -> JsString(movement.state.stage.toString),
        "gender"  -> JsString(movement.state.gender.toString),
        "dir"     -> JsNumber(movement.state.directionAngle.toInt)
      ))
    }
  }

  implicit object StoneFormat extends RootJsonFormat[Stone] {
    override def read(json: JsValue): Stone = ???
    override def write(stone: Stone): JsValue = {
      JsObject(Map(
        "pos" -> JsArray(Vector(stone.pos.x, stone.pos.y).map(_.toInt).map(JsNumber.apply))
      ))
    }
  }

  implicit val arenaUpdatesFormat = jsonFormat3(ArenaUpdates.apply)

  implicit val wsCommandSpawnFormat = jsonFormat1(WebsocketConnection.Spawn)
  implicit val wsCommandKillFormat = jsonFormat1(WebsocketConnection.Kill)
  implicit val wsCommandPutStoneFormat = jsonFormat1(WebsocketConnection.PutStone)
  implicit val wsCommandRemoveStoneFormat = jsonFormat1(WebsocketConnection.RemoveStone)
}
