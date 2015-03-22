package entities

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import ladybugs.calculation.Vec2d
import ladybugs.entities.{LadybugArena, Position}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class LadybugArenaSpec extends TestKit(SQSDeleterSpec.actorSystem)
  with WordSpecLike
  with ImplicitSender
  with BeforeAndAfterAll
  with Matchers {

  "LadybugArena" should {

    "allow movements outside of bounds as long as the movement is going more into bounds" in {
      val arenaRef = TestActorRef[LadybugArena](LadybugArena.props(100, 200))
      val arena = arenaRef.underlyingActor

      val pos1 = Position(Vec2d(0, 0))
      arena.movementWithinBounds(pos1.copy(pos = pos1.pos + Vec2d(-1, -1)), pos1) shouldBe false
      arena.movementWithinBounds(pos1.copy(pos = pos1.pos + Vec2d(-1,  0)), pos1) shouldBe false
      arena.movementWithinBounds(pos1.copy(pos = pos1.pos + Vec2d( 0, -1)), pos1) shouldBe false
      arena.movementWithinBounds(pos1.copy(pos = pos1.pos + Vec2d( 1,  0)), pos1) shouldBe true
      arena.movementWithinBounds(pos1.copy(pos = pos1.pos + Vec2d( 0,  1)), pos1) shouldBe true
      arena.movementWithinBounds(pos1.copy(pos = pos1.pos + Vec2d( 1,  1)), pos1) shouldBe true

      val pos2 = Position(Vec2d(100, 0))
      arena.movementWithinBounds(pos2.copy(pos = pos2.pos + Vec2d(-1, -1)), pos2) shouldBe false
      arena.movementWithinBounds(pos2.copy(pos = pos2.pos + Vec2d(-1,  0)), pos2) shouldBe true
      arena.movementWithinBounds(pos2.copy(pos = pos2.pos + Vec2d( 0, -1)), pos2) shouldBe false
      arena.movementWithinBounds(pos2.copy(pos = pos2.pos + Vec2d( 1,  0)), pos2) shouldBe false
      arena.movementWithinBounds(pos2.copy(pos = pos2.pos + Vec2d( 0,  1)), pos2) shouldBe true
      arena.movementWithinBounds(pos2.copy(pos = pos2.pos + Vec2d(-1,  1)), pos2) shouldBe true

      val pos3 = Position(Vec2d(0, 200))
      arena.movementWithinBounds(pos3.copy(pos = pos3.pos + Vec2d(-1,  1)), pos3) shouldBe false
      arena.movementWithinBounds(pos3.copy(pos = pos3.pos + Vec2d(-1,  0)), pos3) shouldBe false
      arena.movementWithinBounds(pos3.copy(pos = pos3.pos + Vec2d( 0,  1)), pos3) shouldBe false
      arena.movementWithinBounds(pos3.copy(pos = pos3.pos + Vec2d( 1,  0)), pos3) shouldBe true
      arena.movementWithinBounds(pos3.copy(pos = pos3.pos + Vec2d( 0, -1)), pos3) shouldBe true
      arena.movementWithinBounds(pos3.copy(pos = pos3.pos + Vec2d( 1, -1)), pos3) shouldBe true

      val pos4 = Position(Vec2d(100, 200))
      arena.movementWithinBounds(pos4.copy(pos = pos4.pos + Vec2d( 1,  1)), pos4) shouldBe false
      arena.movementWithinBounds(pos4.copy(pos = pos4.pos + Vec2d( 1,  0)), pos4) shouldBe false
      arena.movementWithinBounds(pos4.copy(pos = pos4.pos + Vec2d( 0,  1)), pos4) shouldBe false
      arena.movementWithinBounds(pos4.copy(pos = pos4.pos + Vec2d(-1,  0)), pos4) shouldBe true
      arena.movementWithinBounds(pos4.copy(pos = pos4.pos + Vec2d( 0, -1)), pos4) shouldBe true
      arena.movementWithinBounds(pos4.copy(pos = pos4.pos + Vec2d(-1, -1)), pos4) shouldBe true
    }

  }

  override def afterAll() {
    super.afterAll()
    system.shutdown()
  }
}

object SQSDeleterSpec {
  def actorSystem = ActorSystem("system")
}