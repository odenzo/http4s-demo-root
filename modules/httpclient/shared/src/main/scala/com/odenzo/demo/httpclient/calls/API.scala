package com.odenzo.demo.httpclient.calls

import cats.effect.*
import cats.effect.syntax.all.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import cats.implicits.catsSyntaxApplicativeId
import org.http4s.*
import org.http4s.Method.*
import org.http4s.implicits.*
import org.typelevel.ci.CIString

import java.time.LocalDate
import scala.util.chaining.{*, given}
import scala.xml.Elem

/** These are the API definitions which are independent of the actual HTTP4S client and only depend on HTTP4S crosss platform stuff plus the
  * scala-xml / xxml ENDODER/DECODER We could leave the client defn out of this completely and just bring in a Client[IO] supertype
  */
object API:
  val baseUri                   = uri"http://localhost:8888"
  val acceptXmlHeaders: Headers = Headers(Header.Raw(CIString("Accept"), "application/xml"))

  def postXmlText(xmlStr: String): Request[IO] = Request[IO](POST, (baseUri / "xml" / "echo"), headers = acceptXmlHeaders)

  def postXml(xml: Elem): Request[IO] = Request[IO](POST, (baseUri / "xml" / "echo"), headers = acceptXmlHeaders)

  def ping(): Request[IO] = Request[IO](GET, (baseUri / "xml" / "ping"))

  def getHelloInXML: Request[IO] =
    Request[IO](GET, (baseUri / "xml" / "hello"), headers = acceptXmlHeaders)
