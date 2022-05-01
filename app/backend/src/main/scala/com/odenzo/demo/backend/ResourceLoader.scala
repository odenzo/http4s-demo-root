package com.odenzo.demo.backend

import cats.effect.*
import cats.effect.syntax.all.*
import cats.*
import cats.data.*
import cats.implicits.*
import com.odenzo.base.IOU

import java.io.File as JFile
import scala.jdk.CollectionConverters.*
import java.net.{ProtocolFamily, URI, URL}
import java.nio.file.{Files as JFiles, Path as JPath}
import java.util
import scala.xml.Elem
import fs2.io.*
import fs2.io.file.*

/** We pack files under resources, and need to enumerate in JAR or in IDE/Filesystem. Attempt with Scala/Java and see if fs2-io can help
  * even though the files are so small not really worth streaming. When running in IDE for test its
  * `/Users/stevef/Programming/GitHub/http4s-dom-xml-demo/app/backend/target/scala-3.1.1/classes``
  */
object ResourceLoader {
  val baseDir = "web/testdata"

  def baseUrl: IO[URL] = IOU.exactlyOne("Multiple Locations for Resources $baseDir")(findResources(baseDir))

  /** e.g. web/dir/dir */
  def findResources(pattern: String): List[URL] = this.getClass.getClassLoader.getResources(pattern).asScala.toList

  /** Given a resource, if its a directory this returns a Vector of contents. If its an actual file the a Vector of (1 I think) strings.
    * This maybe chunked and you will have to combine strings. This works with directories, I would think with JAR files too but not tested.
    * We could do everything with resource name instead of paths and URLs, probably better
    * @param name
    *   Resource name, e.g. web/testdata , web/testdata/ , web/testdata/aFile.xml
    * @return
    */
  def loadResourceAsText(name: String): fs2.Stream[IO, String] = readClassLoaderResource[IO](name).through(fs2.text.utf8.decode)

  /** Binary stream straight through */
  def pathToBinaryStream(p: Path): IO[fs2.Stream[IO, Byte]] =
    JFiles.isDirectory(p.toNioPath) match {
      case true  => IO.raiseError(Throwable(s"Path is Directory Not File: $p"))
      case false => IO(Files[IO].readAll(p))
    }

  /** Currently loads as UTF-8 without checking BOM or xml encoding etc. */
  def pathToStringStream(p: Path): IO[fs2.Stream[IO, String]] =
    pathToBinaryStream(p).map(_.through(fs2.text.utf8.decode))

  // Dunno how to make a through for this, almost need an evalThrough to make the Pipe/Stream to go through
  def pathToString(p: Path): IO[String] =
    pathToStringStream(p).flatMap(_.compile.string)

  def pathParsedAndSerialized(url: URL): IO[String] =
    IO.blocking(scala.xml.XML.load(url))
      .map(elem => ScalaXMLParsing.serialize(elem))

  // Well, what do we really want to do? Get a base resource directory and iteratte all the files in it.
  // then we can filter those. Lets learn how to use recursion on FS2 Streams!
  // Pattern should end in "/" if its a directory/container.
  def allFilesUnder(url: URL): fs2.Stream[IO, Path] = {
    val jpath = JPath.of(url.toURI)
    fs2.io.file.Files[IO].walk(jpath).map(jp => fs2.io.file.Path.fromNioPath(jp))
  }

  def allXmlFilesInResource(resDir: String): fs2.Stream[IO, Path] =
    val nStreams: List[fs2.Stream[IO, Path]] = findResources(resDir).map(allFilesUnder)
    val appended: fs2.Stream[IO, Path]       = nStreams.reduceLeft((a, b) => a ++ b)
    appended.filter(p => p.extName == ".xml")

  def allOasisFiles: fs2.Stream[IO, Path] = allXmlFilesInResource("web/testdata/oasis")

  /** Could actually go offs its TOC file but this easier. */
  def validOasisFiles: fs2.Stream[IO, Path]   = allOasisFiles.filter(p => p.fileName.toString.contains("pass"))
  def invalidOasisFiles: fs2.Stream[IO, Path] = allOasisFiles.filter(p => p.fileName.toString.contains("fail"))

  def allValidTests(): fs2.Stream[IO, URI] =
    allXmlFilesInResource("web/testdata/valid").filter(p => p.extName == ".xml").map(_.toNioPath.toUri)
}
