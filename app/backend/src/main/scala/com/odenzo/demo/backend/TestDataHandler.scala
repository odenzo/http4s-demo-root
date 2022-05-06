package com.odenzo.demo.backend

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.implicits.*
import com.odenzo.base.IOU
import com.odenzo.demo.backend.ScalaXMLParsing
import fs2.io.*
import fs2.io.file.*

import java.io.File as JFile
import java.net.{ProtocolFamily, URI, URL}
import java.nio.file.{FileSystem, FileSystems, PathMatcher, Files as JFiles, Path as JPath}
import java.nio.file.spi.FileSystemProvider
import java.util
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.*
import scala.util.chaining.*
import scala.xml.Elem

/** Handler for File Based Resources (e.g. from IDE and external directories)
  */
class TestDataHandler(val baseDir: Path) {
  scribe.info(s"TestDataHandler @ $baseDir")
  import cats.effect.unsafe.implicits.global

  import java.net.URLClassLoader

  def resolved(p: Path): Path = if p.isAbsolute then p else baseDir.resolve(p)

  def tokenAsTextStream(location: Path): IO[fs2.Stream[IO, String]] =
    tokenAsBinaryStream(location).map(_.through(fs2.text.utf8.decode))

  /** @oaram
    *   location -- Full absolute Path of a file.
    * @return
    */
  def tokenAsBinaryStream(path: Path): IO[fs2.Stream[IO, Byte]] = {
    val files: Files[IO] = fs2.io.file.Files[IO]
    val p                = resolved(path)
    for {
      isDir <- files.isDirectory(p)
      _     <- IO.raiseWhen(isDir)(IllegalArgumentException(s"Location $p was an directory!"))
      stream = Files[IO].readAll(p)
    } yield stream
  }

  // Glue
  def loadTokenAsString(location: Path): IO[String] =
    tokenAsTextStream(location).flatMap { (stream: fs2.Stream[IO, String]) => stream.compile.string }

  def loadTokenAsBinary(location: Path): IO[Vector[Byte]] =
    tokenAsBinaryStream(location).flatMap { (stream: fs2.Stream[IO, Byte]) => stream.compile.toVector }

//  // TODO: Remove from here
//  def pathParsedAndSerialized(url: URL): IO[String] =
//    // Or xml.InputSource or InputStream
//    IO.blocking(scala.xml.XML.load(url))
//      .map(elem => ScalaXMLParsing.serialize(elem))

  /** Note this does no security checks or anything at all, just a hack for testing in browser */
  def allFilesUnder(path: Path): IO[fs2.Stream[IO, Path]] = IO {
    scribe.info(s"All Files Under $path (Resolved: ${resolved(path)} ")
    // fs2.io.readClassLoaderResource()
    fs2.io.file.Files[IO].walk(resolved(path)).filter(p => !JFiles.isDirectory(p.toNioPath))
  }

  /** So, there are really a few cases,
    *   - all on JVM:
    *     1. TestFile => scala.xml parsing (of text) 2. TestFile => xxml proxied parsing (of text) 3. TestFile => scala.xml loading file
    *        directly. 4. TestFile => binary load, scala.xml parsing 5. TestFIle => scala.xml direct parsing (dupes 3?)
    *
    *   - On the browser
    *     - Binary straight from file -> xml parsing
    *     - Text assumed in standard encoding (maybe BOM checked and <?xml encoding checked)
    */
//  def parsedAndSerialized(location: Path): IO[fs2.Stream[IO, String]] =
//    val path = resolved(location)
//    for {
//      xmlText <- loadTokenAsString(path) // UTF_8 ONLY NOW!
//      xmlBin <- loadTokenAsBinary(path)
//      sXml   <- JDKParsing.parseFromPathDirectly(path)
//    } yield ()

}

object TestDataHandler {

  /** This is configured to deal with `runrun` where the current working directory is $project/app/backend.
    */
  def cwd(): IO[TestDataHandler] =
    val cwd: IO[Path] = Files[IO].currentWorkingDirectory
    cwd.map(base => new TestDataHandler(base / ".." / ".." / "testdata"))

  def apply(p: Path): TestDataHandler = new TestDataHandler(p)
}
