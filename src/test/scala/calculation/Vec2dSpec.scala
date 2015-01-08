package calculation

import ladybugs.calculation.Vec2d
import org.scalatest._

class Vec2dSpec extends WordSpec with Matchers {

  "A Vec2d" should {

    "do addition correctly" in {
      (Vec2d(1, 1) + Vec2d(1, 1)) shouldBe Vec2d(2, 2)
      (Vec2d(-1, -1) + Vec2d(-1, -1)) shouldBe Vec2d(-2, -2)
    }

    "do subtraction correctly" in {
      (Vec2d(1, 1) - Vec2d(1, 1)) shouldBe Vec2d(0, 0)
      (Vec2d(-1, -1) - Vec2d(-1, -1)) shouldBe Vec2d(0, 0)
    }

    "do multiplication correctly" in {
      (Vec2d(4, 4) * 2) shouldBe Vec2d(8, 8)
    }

    "do division correctly" in {
      (Vec2d(4, 4) / 2) shouldBe Vec2d(2, 2)
    }

    "negate correctly" in {
      (-Vec2d(1, 1)) shouldBe Vec2d(-1, -1)
      (-Vec2d(-1, -1)) shouldBe Vec2d(1, 1)
    }

    "have correct magnitude" in {
      Vec2d(5, 0).magnitude shouldBe 5
      Vec2d(0, 5).magnitude shouldBe 5
      Vec2d(-5, 0).magnitude shouldBe 5
      Vec2d(0, -5).magnitude shouldBe 5
      Vec2d(5, 5).magnitude shouldBe math.sqrt(5 * 5 + 5 * 5)
    }

    "be normalised correctly" in {
      Vec2d(5, 5).normalised.magnitude shouldBe 1.0 +- 0.000001
    }

    "do dotproduct correctly" in {
      Vec2d(5, 5) dot Vec2d(2, 2) shouldBe 20.0
    }
  }

}
