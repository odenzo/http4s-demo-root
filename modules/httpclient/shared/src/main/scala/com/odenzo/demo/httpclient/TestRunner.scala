package com.odenzo.demo.httpclient

import cats.effect.*
import cats.effect.syntax.all.*
import cats.*
import cats.data.*
import cats.implicits.*
import org.http4s.client.Client
import scala.util.chaining.scalaUtilChainingOps

import scala.xml.Elem
import com.odenzo.demo.httpclient.given_Codec_XmlLocation

/** This will use the same code-path (Client -> Server Calls) for ScalaJS in Browser and for JDK Backend. Even though I c ould just call the
  * route functions directly in HTTP4S like normal testing would
  */
class TestRunner(cb: TestResult => IO[Unit], env: String = "JDK", client: Client[IO]) {

  val webBridge: APIBridge = new APIBridge(client) // IJ really hates this for some reason.

  def run(doneCB: Either[Throwable, Boolean] => Unit): IO[Unit] = {
    val root             = XmlLocation("valid")
    val result: IO[Unit] = for {
      vTests <- webBridge.getTestsInDir(roo)
      _       = scribe.info(s"Got ${vTests.length} valid tests under ${root.path}")
      res    <- vTests.traverse(file => parseTestFile(file, TestXmlType.SXML_TEXT, true))
      _       = scribe.info(s"${TestResult.formatTestResultsSummary(res)}")
    } yield ()
    result.handleError { e =>
      scribe.error("Got An Error Getting Valid Tests:", e)
    }
  }

  /** Returns a TestResult (Success or Error) for simply parsing a file, via calling backend webserver. In IO for effect but no exceptions
    * are raised (IO is always successfully redeemed).
    */
  def parseTestFile(file: XmlLocation, mode: TestXmlType, expectedToPass: Boolean = true): IO[TestResult] = {
    webBridge
      .getXml(file, mode)
      .redeem(
        err => TestErrorResult(file, mode, expectedToPass, err.getMessage, err.some),
        elem => TestSuccessResult(file, mode, expectedToPass, XmlUtils.serialize(elem))
      )
  }


   def doValidTest(file: XmlLocation) = {
    import com.odenzo.xxml.*
    val testMode = TestXmlType.TXT
    val beMode   = TestXmlType.SXML_BINARY
    scribe.info(s"Test Location: $file   file:")
    for {
      elem    <- webBridge.getXml(file, mode)
      _        = scribe.info(s"XMLText: $xxml")
      xxmlText =  XXMLParser.se() // Serializer is where?
//      compare <- webBridge.postXmlAndCompare(file, testMode, xxmlText, beMode).recoverWith { err =>
//                   /* cb(pending.withFailure(s"Failed Fetch", Some(err))) *> */
//                   IO.raiseError(err)
//                 }
    } yield compare
//
//    rezz.redeem(e => scribe.error(s"Got An Error: on file:///$file", e), ok => ok)
//
//  }

}
