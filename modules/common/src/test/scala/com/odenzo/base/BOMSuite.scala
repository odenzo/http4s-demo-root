package com.odenzo.base

import scodec.bits.*

import java.nio.charset.Charset
class BOMSuite extends BaseSuite {

  test("BOM LE") {
    val encoding: Option[Charset] = BOMUtils.detectBom(hex"FF FE FF FA AB")
    scribe.info(s"BOMLE: $encoding")
  }

  test("BOM BE") {
    val encoding: Option[Charset] = BOMUtils.detectBom(hex"FE FF 12 34 AD")
    scribe.info(s"BOMBE: $encoding")
  }
}
