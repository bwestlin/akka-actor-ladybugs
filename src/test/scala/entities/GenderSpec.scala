package entities

import ladybugs.entities.Gender
import org.scalatest.{Matchers, WordSpec}

class GenderSpec extends WordSpec with Matchers {

  "Gender" should {

    "consist of 2 values" in {
      Gender.values.size shouldBe 2
    }

    "produce approximately as many males as females" in {

      val randomGenders = (1 to 10000).map(_ => Gender.random)

      val maleGenderCount = randomGenders.count(_ == Gender.male).toDouble
      val femaleGenderCount = randomGenders.count(_ == Gender.female).toDouble

      (maleGenderCount / femaleGenderCount) shouldBe 1.0 +- 0.1
    }
  }
}