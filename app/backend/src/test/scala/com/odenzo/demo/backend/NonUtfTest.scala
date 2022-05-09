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

class NonUtfTest extends munit.CatsEffectSuite {

  val currDir = Files[IO].currentWorkingDirectory.unsafeRunSync()
  scribe.warn(s"Current WD: $currDir")

  def makeHandler = IO.pure(TestDataHandler.apply(currDir / "testdata"))

  val handler = makeHandler.unsafeRunSync()

  test(name = "BOMS") {
    handler
      .allFilesUnder(Path("valid") / "nonUTF8")
      .flatMap { stream =>
        stream
          .debug(f => s"Location: $f")
          .map(f => handler.baseDir.relativize(f))
          .debug(f => s"Relative: $f")
          .compile
          .drain
      }
  }

}
