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

trait ResponseUtils {
  // This is empty and not sure why Scala JS is complaining.
  val xmlContentType: `Content-Type` = `Content-Type`(MediaType.application.xml)

}
