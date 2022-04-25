package com.odenzo.base
import munit.CatsEffectSuite

import scala.concurrent.duration.{Duration, FiniteDuration}

abstract class BaseSuite extends CatsEffectSuite:
  override val munitTimeout: FiniteDuration = Duration(1, "min")
  override def munitTestTransforms: List[TestTransform] = super.munitTestTransforms // ++ List(???)
// ...
//ScribeConfigForTesting.ciSilence
//class MyFirstSuite       extends BaseSuite      { /* ... */}
