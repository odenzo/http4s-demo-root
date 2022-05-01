package com.odenzo.demo.backend

import cats.effect.*
import cats.effect.syntax.all.*
import cats.*
import cats.data.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import fs2.io.file.{Files, Path}

import java.net.URL
import java.nio.file.{Path as JPath, Paths as JPaths}
import scala.util.Try
import scala.xml.Elem

class ResourceLoaderTest extends munit.CatsEffectSuite {

  test("FS2 Dir") {
    val path             = "web/testdata/standalone/001.xml"
    val sres: IO[String] = ResourceLoader.loadResourceAsText(path).compile.string
    sres.flatTap(sr => IO(println(s"$path => $sr")).void)
  }

  test("FS2 Walk & JVM Parse") {

    given IORuntime = IORuntime.global
    // given IO                         = summon[IO]

    val stream: fs2.Stream[IO, Path] = ResourceLoader.findResources("web/testdata").headOption match
      case Some(url) => ResourceLoader.allFilesUnder(url)
      case None      => throw Throwable("No TestData Found")

    /** Each Elem is the root of a new document */
    val result: IO[Vector[(Path, String, Either[Throwable, xml.Elem])]] = stream
      .filter(p => p.extName.endsWith("xml") && !java.nio.file.Files.isDirectory(p.toNioPath))
      .chunkLimit(1)
      .unchunks
      .evalMap(path => ResourceLoader.pathToString(path).map(s => (path, s)))
      .map { case (path, xml) =>
        val attempt = Try {
          ScalaXMLParsing.parse(xml)
        }.toEither
        (path, xml, attempt)
      }
      .debug { formatResults.tupled }
      .compile
      .toVector
    result
  }

  test("All Valid Locations") {
    ResourceLoader.allValidTests().debug(uri => uri.toString).compile.drain
  }

  def formatResults(path: Path, xml: String, res: Either[Throwable, Elem]) = {
    val shouldFail = if path.fileName.toString.contains("fail") then "EXPECTED FAILURE" else "EXPECTED SUCCESS"
    res match {
      case Left(err)   => s"FAILED $shouldFail: ${err.getMessage} \non $path \nWith XML: $xml"
      case Right(elem) => s"$path succeeded $shouldFail"
    }
  }
}
