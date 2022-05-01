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
import com.odenzo.demo.backend.ResourceLoader
import fs2.io.file
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

object XMLTestRoutes extends RouteUtils {

  import java.nio.file.{Files as JFiles, Path as JPath}
  import cats.effect.Ref.*
  import cats.effect.unsafe.implicits.global
  import org.http4s.EntityEncoder.*
  import org.http4s.scalaxml.*
  import org.http4s.headers.*
  import scala.xml.*

  /** Convert given URI str to fs2 path, in IO to catch errors */
  private def uriStrToPath(str: String): IO[file.Path] = IO(fs2.io.file.Path.fromNioPath(JPath.of(java.net.URI.create(str))))

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "positiveCases" =>
      import org.http4s.circe.CirceEntityCodec.*
      scribe.info(s"Getting Positive Test Cases")
      for {
        locations <- ResourceLoader.allValidTests().map(_.toString).compile.toVector
        res       <- Ok(locations)
      } yield res

    /** returns 200 with String payload (unparsed/serialized) but with Content Type application/xml Encoding not dealt with. */
    case GET -> Root / "xmlText" :? ResourceUrlQueryParamMatcher(location) => // Plain Text Response
      for {
        path <- uriStrToPath(location) // Should go in validating parameter really
        s   <- ResourceLoader.pathToString(path)
        res <- Ok(s, `Content-Type`(MediaType.text.plain))
      } yield res

    /** returns 200 raw test text for xml, but with content type set to application/xml */
    case GET -> Root / "xml" :? ResourceUrlQueryParamMatcher(location) =>
      for {
        path <- uriStrToPath(location) // Should go in validating parameter really
        s   <- ResourceLoader.pathToString(path)
        res <- Ok(s, `Content-Type`(MediaType.application.xml))
      } yield res

    // Returns in Binary form, with or without BOM and based on unknown char encoding (usually UTF-8)
    case GET -> Root / "xmlBinary" :? ResourceUrlQueryParamMatcher(location) => // Plain Text Response
      import org.http4s.EntityEncoder.streamEncoder
      for {
        path <- uriStrToPath(location) // Should go in validating parameter really
        stream = ResourceLoader.pathToBinaryStream(path)
        res   <- Ok(stream)
      } yield res

    // For good measure, one that parses the XML server side, then sends it back serialized
    case GET -> Root / "xmlSerialized" :? ResourceUrlQueryParamMatcher(location) => // Plain Text Response
      import org.http4s.EntityEncoder.streamEncoder
      for {
        path <- uriStrToPath(location) // Should go in validating parameter really
        stream = ResourceLoader.pathParsedAndSerialized(path.toNioPath.toUri.toURL)
        res   <- Ok(stream)
      } yield res

    case GET -> Root / "xmlPing" => // XML Response
      val reply = <greeting>Hello!</greeting>
      Ok(reply)

  }
}
