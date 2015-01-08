package entities

import ladybugs.entities.{Gender, Stage, LadybugState}
import org.scalatest.{Matchers, WordSpec}

class LadybugStateSpec extends WordSpec with Matchers {

  "LadybugState" should {

    val eggAge   = 50
    val childAge = 100
    val adultAge = 500
    val oldAge   = 2000
    val deadAge  = 2500
    val annihAge = 2600

    val ages = Seq(eggAge, childAge, adultAge, oldAge, deadAge, annihAge)

    "calculate stage from age" in {
      for {
        (age, stage) <- ages zip Stage.values
      }
      yield {
        LadybugState(age = age).stage shouldBe stage
      }
    }

    "calculate fertile based on age, gender and fertility" in {
      for {
        gender    <- Gender.values
        age       <- ages
        fPercent  <- Seq(0, 89, 90, 100)
      }
      yield {
        val fExpected =
          if (age != adultAge) false
          else if (gender == Gender.male) true
          else if (fPercent < 90) false
          else true

        val state = LadybugState(age = age, gender = gender, fertilityPercent = fPercent)

        assert(state.fertile == fExpected, s"state=$state, fExpected=${fExpected}")
      }
    }
  }
}
