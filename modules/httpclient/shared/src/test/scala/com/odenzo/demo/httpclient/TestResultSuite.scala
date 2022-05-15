package com.odenzo.demo.httpclient

import io.circe.*
import io.circe.syntax.*
import com.odenzo.base.BaseSuite
import com.odenzo.demo.httpclient.*
class TestResultSuite extends BaseSuite {

  val sample = List[TestResult](
    TestSuccessResult(XmlLocation("foo"), TestXmlType.TXT_SNIFFED, true, "Matches:Success"),
    TestSuccessResult(XmlLocation("foo"), TestXmlType.TXT_SNIFFED, false, "MisMatch:Success"),
    TestErrorResult(XmlLocation("foo"), TestXmlType.TXT_SNIFFED, true, "MisMatch:Fail"),
    TestErrorResult(XmlLocation("foo"), TestXmlType.TXT_SNIFFED, false, "Matches:Fail"),
    TestFailure("Failed", None),
    TestFailure("Failed", Some(Throwable("InternalError")))
  )

  test("asJson") {
    sample.map(_.asJson).tapEach((trjson: Json) => scribe.info(s"${trjson.spaces4}"))

  }
  test("failures") {
    val res = TestResult.failures(sample)
    scribe.info(s"Failed: ${pprint(res)}")
    assertEquals(res.length, 2)
  }

  test("correct") {
    val res = TestResult.correctResult(sample)
    scribe.info(s"Correct: ${pprint(res)}")
    assertEquals(res.length, 2)
  }

  test("incorrect") {
    val res = TestResult.incorrectResult(sample)
    scribe.info(s"INCORRECT: ${pprint(res)}")
    assertEquals(res.length, 2)
  }

}
