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

    val fertileNonPregnantFemale = LadybugState(gender = Gender.female, age = adultAge, fertilityPercent = 90, eggs = 0)

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
        val fertileExpected =
          if (age != adultAge) false
          else if (gender == Gender.male) true
          else if (fPercent < 90) false
          else true

        val state = LadybugState(age = age, gender = gender, fertilityPercent = fPercent)

        assert(state.fertile == fertileExpected, s"state=$state, fertileExpected=$fertileExpected")
      }
    }

    "calculate pregnant based on gender, birthTime and eggs" in {
      for {
        gender    <- Gender.values
        birthTime <- Seq(-1, 0, 1)
        eggs      <- Seq(0, 1, 2)
      }
      yield {
        val pregnantExpected =
          if (gender == Gender.male) false
          else if (birthTime < 0) false
          else if (eggs <= 0) false
          else true

        val state = LadybugState(gender = gender, birthTime = birthTime, eggs = eggs)

        assert(state.pregnant == pregnantExpected, s"state=$state, pregnantExpected=$pregnantExpected")
      }
    }

    "answer whether pregnancy is possible based on gender, being fertile and not pregnant" in {
      fertileNonPregnantFemale.pregnancyPossible shouldBe true
      fertileNonPregnantFemale.copy(gender = Gender.male).pregnancyPossible shouldBe false
    }

    "become pregnant in the right circumstances" in {
      for {
        otherGender <- Gender.values
        otherStage  <- Stage.values
        _           <- 1 to 20 // Repeat to test randomizity
      } {
        val pregnantStage = fertileNonPregnantFemale.tryBecomePregnant(otherGender, otherStage)
        if (otherGender == Gender.male && otherStage == Stage.adult) {
          pregnantStage.birthTime shouldBe 200
          pregnantStage.eggs shouldBe 2 +- 1
        }
        else {
          pregnantStage.birthTime shouldBe 0
          pregnantStage.eggs shouldBe 0
        }
      }
    }
  }
}
