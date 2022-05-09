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

    case GET -> Root / "xml" :? PathFileQueryParam(file) :? XmlTypeQueryParam(mode)       =>
      import org.http4s.EntityEncoder.*
      scribe.info(s"SERVER: $mode @ $file")
      mode match {
        case TXT => resourceLoader.loadTokenAsString(file).flatMap(s => Ok(s, xmlContentType))

        case TXT_SNIFFED =>
          val stream = resourceLoader.tokenAsBinaryStream(file)
          // TODO: Finish This with sniffer and converting to stream of char of String
          Ok(stream)

        case SXML_TEXT => resourceLoader.scalaXmlParsedAndSerialized(file).flatMap(Ok(_, xmlContentType))

        case other => BadRequest(s"Test Type Not Implemented: $other")

      }
    /** Intent is for Browser parsing of a location, then sending the serialized result back. So we ....
      */
    case rq @ POST -> Root / "xml" :? PathFileQueryParam(file) :? XmlTypeQueryParam(mode) =>
      import com.odenzo.xxml.{given, *}
      for {
        saxml      <- ScalaXMLParsing.parseFromPath(file)
        browserXml <- rq.as[scala.xml.Elem]
        res        <- Ok("NotCompleted")
      } yield res

  }
}
