package com.odenzo.demo.backend

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*

import fs2.io.file.{Files, Path}
import munit.*
import munit.CatsEffectSuite.{*, given}

import java.net.URL
import java.nio.file.{Files as JFiles, Path as JPath, Paths as JPaths}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try
import scala.xml.Elem

class TestDataHandlerTest extends munit.CatsEffectSuite {

  def makeHandler = TestDataHandler.cwd()
  val handler     = makeHandler.unsafeRunSync()

  test("Load Relative Text") {
    for {
      loader <- makeHandler
      path    = "standalone/001.xml"
      sres   <- loader.loadTokenAsString(Path(path))
      _       = scribe.info(s"$path => $sres")
    } yield sres
  }

  test(name = "FilesUnderPath") {
    handler
      .allFilesUnder(Path("valid"))
      .flatMap { stream =>
        stream
          .debug(f => s"Location: $f")
          .map(f => handler.baseDir.relativize(f))
          .debug(f => s"Relative: $f")
          .compile
          .drain
      }
  }

  // Just go thru and make sure we can load all the valid ones as raw text or binary.
  // No XML specific checking at all, just care about? No Exceptions, or zero lengths?
  test("SanityLoadingAllTestAndBinary") {
    for {
      loader  <- makeHandler
      resource = "valid"
      stream  <- loader.allFilesUnder(Path(resource))
      _       <- stream
                   .debug(s => s"Location: $s")
                   .evalMap(location => IO.both(loader.loadTokenAsString(location), loader.loadTokenAsBinary(location)))
                   .evalTap((v: (String, Vector[Byte])) => IO(scribe.info(s"Loaded TxtLen: ${v._1.length} Bytes: ${v._2.size} ")))
                   .compile
                   .drain
    } yield stream
  }

  test("SanityLoadingAllTestAndBinary") {
    for {
      loader  <- makeHandler
      resource = "valid"
      stream  <- loader.allFilesUnder(Path(resource))
      _       <- stream
                   .debug(s => s"Location: $s")
                   .evalMap(location => IO.both(loader.loadTokenAsString(location), loader.loadTokenAsBinary(location)))
                   .evalTap((v: (String, Vector[Byte])) => IO(scribe.info(s"Loaded TxtLen: ${v._1.length} Bytes: ${v._2.size} ")))
                   .compile
                   .drain
    } yield stream
  }

  test(name = "WalkBad") {
    for {
      loader  <- makeHandler
      resource = "web/outsideOfDomain/valid"
      stream  <- loader.allFilesUnder(Path(resource))
      _        = stream.debug().compile.drain
    } yield stream
  }

  private def formatResults(path: Path, xml: String, res: Either[Throwable, Elem]) = {
    val shouldFail = if path.fileName.toString.contains("fail") then "EXPECTED FAILURE" else "EXPECTED SUCCESS"
    res match {
      case Left(err)   => s"FAILED $shouldFail: ${err.getMessage} \non $path \nWith XML: $xml"
      case Right(elem) => s"$path succeeded $shouldFail"
    }
  }
}
