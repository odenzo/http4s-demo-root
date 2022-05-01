package com.odenzo.base
import cats.effect.IO

class SecretTest extends munit.CatsEffectSuite:

  test("no io") {
    assert(true)
  }
  test("io") {
    println("Test is Running")
    IO.delay {
      assertEquals(23, 23)
    }
  }
