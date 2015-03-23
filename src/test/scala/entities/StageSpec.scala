package entities

import ladybugs.entities.{LadybugArena, Gender, Stage}
import org.scalatest.{Matchers, WordSpec}

class StageSpec extends WordSpec with Matchers {

  "Stage" should {

    "consist of 6 values" in {
      Stage.values.size shouldBe 6
    }

    "give correct stage from an age" in {
      Stage.fromAge(99) shouldBe Stage.egg
      Stage.fromAge(100) shouldBe Stage.child
      Stage.fromAge(499) shouldBe Stage.child
      Stage.fromAge(500) shouldBe Stage.adult
      Stage.fromAge(1999) shouldBe Stage.adult
      Stage.fromAge(2000) shouldBe Stage.old
      Stage.fromAge(2499) shouldBe Stage.old
      Stage.fromAge(2500) shouldBe Stage.dead
      Stage.fromAge(2599) shouldBe Stage.dead
      Stage.fromAge(2600) shouldBe Stage.annihilated
    }

    "give correct age from a stage" in {
      Stage.ageFromStage(Stage.egg) shouldBe 0
      Stage.ageFromStage(Stage.child) shouldBe 100
      Stage.ageFromStage(Stage.adult) shouldBe 500
      Stage.ageFromStage(Stage.old) shouldBe 2000
      Stage.ageFromStage(Stage.dead) shouldBe 2500
      Stage.ageFromStage(Stage.annihilated) shouldBe 2600
    }
  }
}
