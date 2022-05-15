package com.odenzo.demo.backend

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.implicits.*
import com.odenzo.base.{BOMUtils, IOU}
import com.odenzo.demo.backend.ScalaXMLParsing
import com.odenzo.demo.httpclient.TestXmlType
import com.odenzo.demo.httpclient.TestXmlType.{BINARY, SXML_BINARY, SXML_TEXT, TXT, TXT_SNIFFED}
import fs2.io.*
import fs2.io.file.*

import java.io.File as JFile
import java.net.{ProtocolFamily, URI, URL}
import java.nio.charset.{CharacterCodingException, StandardCharsets}
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

  /** Iff p not absolute resolves to configured/injected base data directory. */
  def resolved(p: Path): Path = if p.isAbsolute then p else baseDir.resolve(p)

  /** Load file as UTF-8 */
  def tokenAsTextStream(location: Path): IO[fs2.Stream[IO, String]] =
    tokenAsBinaryStream(location).map(_.through(fs2.text.utf8.decode))

  def loadContentAsTextSniffed(file: Path): IO[String] =
    for {
      vb         <- loadTokenAsBinary(file)
      bv          = scodec.bits.ByteVector(vb)
      maybeBOMEnc = BOMUtils.detectBom(bv)
      encoding    = BOMUtils.detectXmlPiEncoding(bv)
      _           = scribe.info(s"BOM Encoding $maybeBOMEnc  XmlPI ENcoding: $encoding")
      enc         = maybeBOMEnc.orElse(encoding).getOrElse(StandardCharsets.UTF_8)
      txt        <- bv.decodeString(enc) match
                      case Left(err)    => IO.raiseError(Throwable(s"Error Reading $file at text", err))
                      case Right(value) => IO.pure(value)
    } yield txt

  /** @param path
    *   File to read as binary
    * -- Full absolute Path of a file.
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

  /** Gets ScalaXML to load directly from Path, no sniffing etc, and then serializes the Elem */
  def scalaXmlParsedAndSerialized(file: Path): IO[String] =
    // Might need to resolve this to absolute to be safe.
    ScalaXMLParsing
      .parseFromPath(resolved(file))
      .map(elem => ScalaXMLParsing.serialize(elem))

  /** Get all the *.xml test files recursively under the given directory path. Note: this does no security checks or anything at all, just a
    * hack for testing in browser.
    */
  def allFilesUnder(dir: Path): IO[fs2.Stream[IO, Path]] = IO {
    scribe.info(s"All Files Under $dir (Resolved: ${resolved(dir)} ")
    fs2.io.file.Files[IO].walk(resolved(dir)).filter(p => !JFiles.isDirectory(p.toNioPath)).filter(_.extName == ".xml").debug()
  }

  def pathAsText(file: Path, mode: TestXmlType): IO[String] = {
    // IO.raiseUnless(Files[Id].isDirectory(file))(IO.raiseError(Throwable(s"$file is not a Dir")))

    mode match
      case TXT         => loadTokenAsString(file) // This will fuck up the encoding if not UTF-8
      case BINARY      => IO.raiseError(Throwable("Not Supporting Pure Byte"))
      case SXML_TEXT   =>
        loadTokenAsString(file)
          .flatMap(ScalaXMLParsing.parseFromString)
          .map(ScalaXMLParsing.serialize)
      case SXML_BINARY =>
        ScalaXMLParsing
          .parseFromPath(file)
          .map(ScalaXMLParsing.serialize)
      case TXT_SNIFFED =>
        loadContentAsTextSniffed(file)
  }

}

object TestDataHandler {

  /** This is configured to deal with `runrun` where the current working directory is $project/app/backend.
    */
  def cwd(): IO[TestDataHandler] =
    val cwd: IO[Path] = Files[IO].currentWorkingDirectory
    cwd.map(base => new TestDataHandler(base / ".." / ".." / "testdata"))

  def apply(p: Path): TestDataHandler = new TestDataHandler(p)
}
