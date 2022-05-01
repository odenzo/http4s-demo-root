package com.odenzo.demo.backend.routes

import cats.data.*
import cats.*
import cats.effect.*
import cats.syntax.all.*

import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try
import scala.util.chaining.*

// Wait for Scala 3 for enum
sealed trait ResponseFormat extends Product
case object JsonFormat      extends ResponseFormat
case object HTMLFormat      extends ResponseFormat

trait RouteUtils:

  val unknownMimeType = "application/octet-stream"

  object ResourceUrlQueryParamMatcher extends QueryParamDecoderMatcher[String]("location")

  def getHeaderAsString(headers: Headers, headerName: String): IO[String] = IO
    .fromOption(headers.get(CIString(headerName)).map(_.head.value))(Throwable(s"Required HTTP Header $headerName not found"))

  def checkResponseFormat(rq: Request[IO]): ResponseFormat =
    val res = rq.headers.get(CIString("Accept")).map(_.head.value) match
      case Some(s) if s.contains("application/json") => JsonFormat
      case _                                         => HTMLFormat
    res.tap(v => scribe.warn(s"Response Format: $v"))

  object LocalDateVar:
    def unapply(str: String): Option[LocalDate] =
      if str.nonEmpty then Try(LocalDate.parse(str)).toOption
      else None

  val OBinaryMediaType: MediaType         = MediaType.application.`octet-stream`
  val OContentType_CSS: `Content-Type`    = `Content-Type`(MediaType.text.css)
  val OContentType_Binary: `Content-Type` = `Content-Type`(OBinaryMediaType)

  def staticFileAllowed(path: String): Boolean = List(".gif", ".js", ".css", ".map", ".html", ".webm").exists(path.endsWith)
