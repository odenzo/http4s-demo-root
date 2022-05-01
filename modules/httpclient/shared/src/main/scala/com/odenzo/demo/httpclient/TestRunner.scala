package com.odenzo.demo.httpclient

import cats.effect.*
import cats.effect.syntax.all.*
import cats.*
import cats.data.*
import cats.implicits.*
import org.http4s.client.Client
import XmlUtils.given
import javax.security.sasl.SaslClientFactory

case class TestResult(uri: String, expectedPass: Boolean, result: Boolean, notes: String, error: Option[Throwable] = None)

/** This will use the same code-path (Client -> Server Calls) for ScalaJS in Browser and for JDK Backend. Even though I c ould just call the
  * route functions directly in HTTP4S like normal testing would
  */
class TestRunner(cb: TestResult => IO[Unit], env: String = "JDK", client: Client[IO]) {

  val svc: APIBridge = new APIBridge(client) // IJ really hates this for some reason.

  def run(doneCB: Either[Throwable, Boolean] => Unit): IO[Unit] = {
    val result: IO[Unit] = for {
      vTests <- svc.getValidTests
      _       = println(s"Got ${vTests.length} valid tests.")
      _      <- vTests.take(5).traverse(location => doValidTest(location))
    } yield ()
    result.handleError { e =>
      scribe.error("Got An Error Getting Valid Tests:", e)
    }
  }

  def doValidTest(location: String) = {
    import com.odenzo.xxml.*
    scribe.info(s"Test Location: $location")
    val single: IO[Unit] = for {
      xmlTxt <- svc.getXmlString(location)
      _       = scribe.info(s"XMLText: $xmlTxt")
      xml    <- svc
                  .getXml(location)
                  .redeemWith(
                    err => cb(TestResult(location, true, false, s"Raw Test:[$xmlTxt]", Some(err))),
                    elem => cb(TestResult(location, true, false, s"Raw Test:[$xmlTxt]"))
                  )
    } yield ()

    single.redeem(e => scribe.error("Got An Error:", e), ok => ok)

  }

}
