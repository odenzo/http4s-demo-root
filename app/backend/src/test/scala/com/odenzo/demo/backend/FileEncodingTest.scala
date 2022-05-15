package com.odenzo.demo.backend

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import fs2.io.file.{Files, Path}
import munit.CatsEffectSuite.{*, given}
import munit.CatsEffectSuite
import java.net.URL
import java.nio.file.{Files as JFiles, Path as JPath, Paths as JPaths}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try
import scala.xml.Elem

class FileEncodingTest extends munit.CatsEffectSuite with XmlHarness {

  import java.nio.charset.StandardCharsets.UTF_8

  val currDir: Path = Files[IO].currentWorkingDirectory.unsafeRunSync()
  scribe.warn(s"Current WD: $currDir")

  def makeHandler: IO[TestDataHandler] = IO.pure(TestDataHandler.apply(currDir / "testdata"))

  val handler: TestDataHandler = makeHandler.unsafeRunSync()

  given TestDataHandler = handler

  val base = currDir / "testdata"

  scribe.info(s"Looping Around")
  handler
    .allFilesUnder(Path("encodings/PI_MATCHING_BOM"))
    .flatMap((s: fs2.Stream[IO, Path]) => s.compile.toList)
    .map { (v: List[Path]) =>
      v.map { path =>
        scribe.info(s"Doing the Checked for file://$path")
        encodingSniffingCheck(path)
        encodingDirectCheck(path)
      }
    }
    .unsafeRunSync()

  def encodingSniffingCheck(p: Path)(using loc: munit.Location): Unit = {
    val shortPath = currDir.relativize(p)
    test(s"Sniffin $shortPath") {
      val res: IO[Unit] = parseOneFromPathSniffed(p)
      res
    }
  }

  def encodingDirectCheck(p: Path)(using loc: munit.Location): Unit = {

    val shortPath: Path = currDir.relativize(p)
    scribe.info(s"Direct:\t $shortPath")
    test(s"Direct $shortPath") {
      parseOneFromPathDirect(p)
    }
  }

  def parseOneFromPathDirect(p: Path): IO[Unit] = {
    fs2
      .Stream(p)
      .debug(f => s"Location: $f")
      .evalMap(xmlTxt => ScalaXMLParsing.parseFromPath(p))
      .evalTap(elem => IO(scribe.info(s"From Path Direct -> $elem")))
      .compile
      .drain
  }

  def parseOneFromPathSniffed(p: Path): IO[Unit] = {
    fs2
      .Stream(p)
      .debug(f => s"Location: $f")
      .evalMap(p => handler.loadContentAsTextSniffed(p))
      .debug(f => s"XML Text: $f")
      .evalMap(xmlTxt => ScalaXMLParsing.parseFromString(xmlTxt))
      .evalTap(elem => IO(scribe.info(s"-> $elem")))
      .compile
      .drain
  }

}
