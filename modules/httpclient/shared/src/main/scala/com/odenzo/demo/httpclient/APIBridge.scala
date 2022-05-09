package com.odenzo.demo.httpclient

import cats.effect.IO
import cats.effect.syntax.all.{*, given}
import cats.syntax.all.{*, given}
import com.odenzo.xxml.*
import fs2.io.file.Path as FPath
import io.circe.Json
import org.http4s.Method.{GET, POST}
import org.http4s.headers.*
import org.http4s.syntax.all.{*, given}
import org.http4s.*
import org.http4s.circe.jsonOf
import org.http4s.client.Client
import org.http4s.client.middleware.Logger
import org.typelevel.ci.CIString

import scala.xml.Elem
import com.odenzo.demo.httpclient.*
import org.http4s.dsl.io.QueryParamDecoderMatcher

class APIBridge(client: Client[IO]) extends RequestUtils {
  import com.odenzo.demo.httpclient.APIBridge.baseUri
  object PathFileQueryParam extends QueryParamDecoderMatcher[XmlLocation]("file")
  object PathDirQueryParam  extends QueryParamDecoderMatcher[XmlLocation]("dir")

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

  def sayHello: IO[String]           = Request[IO](GET, baseUri / "xml" / "ping").as[String]
  def sayHelloWithXmlRq: Request[IO] = Request[IO](GET, baseUri / "xml" / "xmlPing").putHeaders(acceptJsonXmlHeader)
  def sayHelloWithXml: IO[Elem]      = client.expect[Elem](sayHelloWithXmlRq)

  // === TESTING ===
  def getValidTests: IO[List[XmlLocation]] = getTestsInDir(XmlLocation("valid"))

  /** This can be absolute or relative directory. */
  def getTestsInDir(dir: XmlLocation): IO[List[XmlLocation]] =
    import io.circe.Decoder.*
    // import io.circe.generic.
    import com.odenzo.demo.httpclient.given
    import org.http4s.circe.jsonDecoder
    given EntityDecoder[IO, List[XmlLocation]] = jsonOf[IO, List[XmlLocation]]
    for {
      _     <- IO(scribe.info(s"Getting Tests in $dir"))
      theUri = baseUri.withPath(path"tests/list").withQueryParam("dir", dir)
      rq     = Request[IO](GET, theUri).putHeaders(acceptJsonXmlHeader)
      rs    <- client.expect[List[XmlLocation]](rq)
    } yield rs

  def getXmlRq(location: XmlLocation, mode: TestXmlType = TestXmlType.TXT)(using qpe: QueryParamEncoder[XmlLocation]): Request[IO] =
    scribe.info(s"About to Request $mode @  $location ")

    Request[IO](
      GET,
      (baseUri / "tests" / "xml")
        .withQueryParam("file", location)
        .withQueryParam("xmlType", mode.toString)
    )

  val getXml: (XmlLocation, TestXmlType) => IO[Elem] = Function.untupled((getXmlRq _).tupled andThen client.expect[Elem])

  def postXmlAndCompate(file: XmlLocation, mode: TestXmlType, elem: scala.xml.Elem)(
      using QueryParamEncoder[XmlLocation],
      QueryParamEncoder[TestXmlType]
  ) =
    Request[IO](
      POST,
      (baseUri / "tests" / "xml")
        .withQueryParam("file", file)
        .withQueryParam("xmlType", mode)
    )
}

object APIBridge {
  protected val baseUri: Uri = uri"http://127.0.0.1:9999/" // Hack, sue me

}
