package com.odenzo.demo.httpclient

import cats.effect.IO
import org.http4s.Method.{GET, POST}
import org.http4s.{*, given}
import org.http4s.headers.*
import org.http4s.syntax.all.*
import org.typelevel.ci.CIString

import scala.xml.Elem

import com.odenzo.xxml._

/** These are the API definitions which are independent of the actual HTTP4S client and only depend on HTTP4S crosss platform stuff plus the
  * scala-xml / xxml ENDODER/DECODER We could leave the client defn out of this completely and just bring in a Client[IO] supertype
  */
object API:
  val baseUri                   = uri"http://localhost:9999" // Hack, sue me
  val acceptXmlHeaders: Headers = Headers(Header.Raw(CIString("Accept"), "application/xml"))

  /** This will post the XML text, but with content type of String */
  def postXmlText(xmlStr: String): Request[IO] = Request[IO](POST, baseUri / "xml" / "echo", headers = acceptXmlHeaders)
    .withEntity(xmlStr)
    .withContentType(`Content-Type`(MediaType.application.xml))

  /** This needs an EntityEncoder for Elem this the imports of xxml._, on JVM now it aliases to http4s original ones, and on ScalaJS uses
    * new implicit ones.
    */
  def postXml(xml: Elem): Request[IO] = Request[IO](POST, baseUri / "xml" / "echo", headers = acceptXmlHeaders)
    .withEntity(xml)

  def ping: Request[IO] = Request[IO](GET, baseUri / "xml" / "ping")

  def getHelloInXML: Request[IO] =
    Request[IO](GET, baseUri / "xml" / "xmlPing", headers = acceptXmlHeaders)
