package com.odenzo.demo.backend.routes

import cats.data.*
import cats.*
import cats.effect.*
import cats.syntax.all.*
import fs2.io.file
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Try
import scala.util.chaining.*
import java.nio.file.Path as JPath

import java.nio.charset.Charset as JCharset

trait ResponseUtils {
  // This is empty and not sure why Scala JS is complaining.
  private val utf8                      = java.nio.charset.StandardCharsets.UTF_8
  val xmlContentType: `Content-Type`    = xmlContentType(utf8)
  def xmlContentType(charset: JCharset) = `Content-Type`(MediaType.application.xml, Charset(charset))
}
