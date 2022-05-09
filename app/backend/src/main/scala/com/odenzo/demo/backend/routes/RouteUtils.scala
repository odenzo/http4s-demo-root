package com.odenzo.demo.backend.routes

import cats.data.*
import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.odenzo.demo.httpclient.{RequestUtils, TestXmlType, XmlLocation}
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString
import fs2.io.file.Path as FPath

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try
import scala.util.chaining.*

// Wait for Scala 3 for enum
sealed trait ResponseFormat extends Product
case object JsonFormat      extends ResponseFormat
case object HTMLFormat      extends ResponseFormat

trait RouteUtils extends ResponseUtils with RequestUtils {

  def getHeaderAsString(headers: Headers, headerName: String): IO[String] = IO
    .fromOption(headers.get(CIString(headerName)).map(_.head.value))(Throwable(s"Required HTTP Header $headerName not found"))

  def staticFileAllowed(path: String): Boolean = List(".gif", ".js", ".css", ".map", ".html", ".webm").exists(path.endsWith)

  /** Client Side we use XmlLocation because no FS2 or nio Path */
  given fpathQPCodec: QueryParamCodec[FPath] = QueryParamCodec.from[FPath](
    QueryParamDecoder.stringQueryParamDecoder.emap[FPath] { (str: String) =>
      scribe.info(s"Decoding FPAth from $str")
      val res = ParseResult.fromTryCatchNonFatal(str)(FPath(str))
      scribe.info(s"Decoded To: $res")
      res
    },
    QueryParamEncoder.stringQueryParamEncoder.contramap[FPath](p => p.toString)
  )

  object PathFileQueryParam extends QueryParamDecoderMatcher[FPath]("file")
  object PathDirQueryParam  extends QueryParamDecoderMatcher[FPath]("dir")

}
