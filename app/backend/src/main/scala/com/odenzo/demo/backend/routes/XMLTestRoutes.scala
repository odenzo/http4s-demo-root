package com.odenzo.demo.backend.routes

import io.circe.*
import io.circe.syntax.*
import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.kernel.Outcome
import cats.effect.syntax.all.*
import cats.syntax.all.*
import com.odenzo.base.OPrint.oprint
import com.odenzo.demo.backend.{ScalaXMLParsing, TestDataHandler}
import com.odenzo.demo.httpclient.{TestResult, TestSuccessResult, XmlLocation}
import com.odenzo.demo.httpclient.TestXmlType.*
import fs2.io.file
import fs2.io.file.Path as FSPath
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

class XMLTestRoutes(resourceLoader: TestDataHandler) extends RouteUtils {

  import java.nio.file.{Files as JFiles, Path as JPath}
  import cats.effect.Ref.*
  import cats.effect.unsafe.implicits.global
  import org.http4s.EntityEncoder.*
  import org.http4s.EntityDecoder.*
  import org.http4s.scalaxml.*
  import org.http4s.headers.*
  import scala.xml.*

  /** This is obviously a big security problem, but its demo/test program. Only do this at home! */
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case GET -> Root / "xmlPing" => // XML Response Encoded
      import com.odenzo.xxml.{given, *}
      val reply = <greeting>Hello!　ンチモチキナソ兀山 </greeting>
      Ok(reply)

    case GET -> Root / "list" :? PathDirQueryParam(basedir) =>
      import org.http4s.circe.CirceEntityCodec.*
      scribe.info(s"Getting Test Cases Under: [$basedir]")
      for {
        stream   <- resourceLoader.allFilesUnder(basedir)
        locations = stream.map(_.toString).compile.toVector
        res      <- Ok(locations)
      } yield res

    /** Gets an XML file and transforms (via mode) into XML Text Returned to FrontEnd in correct encoding */
    case GET -> Root / "xml" :? PathFileQueryParam(file) :? BEXmlTypeParam(mode)                                   =>
      import org.http4s.EntityEncoder.*
      scribe.info(s"SERVER: $mode @ $file")
      mode match {
        case TXT_SNIFFED =>
          val txt = resourceLoader.loadContentAsTextSniffed(file) // Might need to strip off XML PI encoding, or change to UTF-8
          txt.flatMap(t => Ok(t, xmlContentType))

        case SXML_TEXT => resourceLoader.scalaXmlParsedAndSerialized(file).flatMap(Ok(_, xmlContentType))

        case other => BadRequest(s"Test Type Not Implemented: $other")

      }
    /** Intent is for Browser parsing of a location, then sending the serialized result back. So we ....
      */
    case rq @ POST -> Root / "xml" :? PathFileQueryParam(file) :? FEXmlTypeParam(feMode) :? BEXmlTypeParam(beMode) =>
      import com.odenzo.xxml.{given, *}
      import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
      for {
        browserXmlTxt <- rq.as[String]
        browserXml    <- ScalaXMLParsing.parseFromString(browserXmlTxt)
        saxml         <- ScalaXMLParsing.parseFromPath(file)
        result = TestSuccessResult(XmlLocation(file.toString), feMode, true, "") // Placeholder
        res           <- Ok(result)
      } yield res

  }

}
