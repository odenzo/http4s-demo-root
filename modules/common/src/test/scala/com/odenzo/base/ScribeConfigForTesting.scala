package com.odenzo.base

object ScribeConfigForTesting:

  /** Appropriate Settings for CI */
  def ciSilence() =
    scribe.info(s"About to set Scribe Level to Warn")
    //  ScribeLoggingConfig.init(Level.Warn)

  /** Appropriate Settings (?) for interactive testing */
  def noisyInTesting = noiseMakers

  val noiseMakers = List("com.zaxxer.hikari.Hikari")
