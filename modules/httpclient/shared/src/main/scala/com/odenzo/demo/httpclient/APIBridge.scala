package com.odenzo.demo.httpclient

import cats.effect.IO
import cats.effect.syntax.all.{*, given}
import cats.syntax.all.{*, given}
import org.http4s.Method.{GET, POST}
import org.http4s.{*, given}
import org.http4s.headers.*
import org.http4s.syntax.all.*
import org.typelevel.ci.CIString
import org.http4s.syntax.all.{*, given}
import org.http4s.client.Client
import com.odenzo.xxml.*
import org.http4s.EntityDecoder
import org.http4s.circe.jsonOf

import scala.xml.Elem
import com.odenzo.xxml.*
import io.circe.Json
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.middleware.Logger

class APIBridge(client: Client[IO]) {
  import APIBridge.*

  /** This will post the XML text, but with content type of String, and get echoed back. */
  def echoAsXml(xmlStr: String): IO[Elem] = {
    import com.odenzo.xxml.given

    val rq = Request[IO](POST, baseUri / "xml" / "echo")
      .withEntity(xmlStr)
      .withContentType(`Content-Type`(MediaType.application.xml))
      .putHeaders(acceptJsonXmlHeader)

    client.expect[Elem](rq)

  }

  /** This needs an EntityEncoder for Elem this the imports of xxml._, on JVM now it aliases to http4s original ones, and on ScalaJS uses
    * new implicit ones.
    */
  def postXmlAndEchoRq(xml: Elem): Request[IO] = Request[IO](POST, baseUri / "xml" / "echo")
    .putHeaders(acceptJsonXmlHeader)
    .withEntity(xml)

  val postXmlAndEcho: Elem => IO[Elem] = (postXmlAndEchoRq _) andThen client.expect[Elem]

  def sayHello: IO[String] = Request[IO](GET, baseUri / "xml" / "ping").as[String]

  def sayHelloWithXmlRq: Request[IO] = Request[IO](GET, baseUri / "xml" / "xmlPing").putHeaders(acceptJsonXmlHeader)
  def sayHelloWithXml: IO[Elem]      = client.expect[Elem](sayHelloWithXmlRq)

  def getValidTests: IO[List[String]] =
    import io.circe.Decoder.*
    // import io.circe.generic.
    import org.http4s.circe.jsonDecoder
    given EntityDecoder[IO, List[String]] = jsonOf[IO, List[String]]
    for {
      _  <- IO(scribe.info(s"Getting Valid Tests!"))
      rq  = Request[IO](GET, baseUri / "tests" / "positiveCases").putHeaders(acceptJsonXmlHeader)
      rs <- client.expect[List[String]](rq)
    } yield rs

  def getXmlStringRq(location: String): Request[IO] = Request[IO](GET, (baseUri / "tests" / "xmlText").withQueryParam("location", location))
  val getXmlString: String => IO[String]            = (getXmlStringRq _) andThen client.expect[String]

  def getXmlRq(location: String): Request[IO] = Request[IO](GET, (baseUri / "tests" / "xmlText").withQueryParam("location", location))
  val getXml: String => IO[Elem]              = (getXmlRq _) andThen client.expect[Elem]

}

object APIBridge {
  protected val baseUri                     = uri"http://127.0.0.1:9999/" // Hack, sue me
  private val range: MediaRange             = MediaRange.`application/*`
  private val lowQ: QValue                  = qValue".750"
  private val xmlMedia: MediaRangeAndQValue = MediaType.application.xml.withQValue(lowQ)
  private val jsonMedia: MediaType          = MediaType.application.json
  private val acceptJsonXmlHeader: Accept   = Accept(jsonMedia, xmlMedia) // Probably text/* would be good too

}
