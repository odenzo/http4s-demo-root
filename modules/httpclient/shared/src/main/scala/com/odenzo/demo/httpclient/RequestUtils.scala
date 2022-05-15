package com.odenzo.demo.httpclient

import cats.data.*
import cats.*
import cats.effect.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.{`Content-Type`, Accept, MediaRangeAndQValue}
import org.typelevel.ci.CIString

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try
import scala.util.chaining.*
import fs2.io.file.Path as FPath
import org.http4s.syntax.all.qValue
import com.odenzo.demo.httpclient.*
trait RequestUtils:
  protected val range: MediaRange             = MediaRange.`application/*`
  protected val lowQ: QValue                  = qValue".750"
  protected val xmlMedia: MediaRangeAndQValue = MediaType.application.xml.withQValue(lowQ)
  protected val jsonMedia: MediaType          = MediaType.application.json
  protected val acceptJsonXmlHeader: Accept   = Accept(jsonMedia, xmlMedia) // Probably text/* would be good too

  given xmlLocationQPCodec: QueryParamCodec[XmlLocation] = QueryParamCodec.from[XmlLocation](
    QueryParamDecoder.stringQueryParamDecoder.map[XmlLocation] { (str: String) => XmlLocation(str) },
    QueryParamEncoder.stringQueryParamEncoder.contramap[XmlLocation](p => p.path.toString)
  )

  given testXmlTypeQPCodec: QueryParamCodec[TestXmlType] = QueryParamCodec.from[TestXmlType](
    QueryParamDecoder.stringQueryParamDecoder.emap { str =>
      ParseResult.fromTryCatchNonFatal(str)(TestXmlType.valueOf(str)).tap(res => scribe.info(s"Type: $res"))
    },
    QueryParamEncoder.stringQueryParamEncoder.contramap[TestXmlType](en => en.toString)
  )

  object FEXmlTypeParam extends QueryParamDecoderMatcher[TestXmlType]("feXmlType")
  object BEXmlTypeParam extends QueryParamDecoderMatcher[TestXmlType]("beXmlType")

  object LocalDateVar:
    def unapply(str: String): Option[LocalDate] =
      if str.nonEmpty then Try(LocalDate.parse(str)).toOption
      else None

  val OBinaryMediaType: MediaType         = MediaType.application.`octet-stream`
  val OContentType_CSS: `Content-Type`    = `Content-Type`(MediaType.text.css)
  val OContentType_Binary: `Content-Type` = `Content-Type`(OBinaryMediaType)
