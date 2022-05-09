package com.odenzo.demo.backend

import cats.effect.IO
import fs2.io.file.{Files as FFiles, Path as FPath}
import fs2.Stream as FStream
import fs2.Pipe as FPipe

import java.nio.charset.Charset
import java.nio.file.Files as JFiles
import scala.xml.Elem

trait XmlHarness {

  def listAllIn(p: FPath)(using handler: TestDataHandler): IO[FStream[IO, FPath]] = handler.allFilesUnder(p)

  def testAllIn(p: FPath)(using handler: TestDataHandler) =
    for {
      stream <- handler.allFilesUnder(p)
      _      <- stream
                  .debug(s => s"Location: $s")
                  .evalMap(location => IO.both(handler.loadTokenAsString(location), handler.loadTokenAsBinary(location)))
                  .evalTap((v: (String, Vector[Byte])) => IO(scribe.info(s"Loaded TxtLen: ${v._1.length} Bytes: ${v._2.size} ")))
                  .compile
                  .drain
    } yield stream

  /** Tests X inidivual paths, all of which should be a readbable File of XML. */
  def testPaths(ps: FPath*)(using handler: TestDataHandler) = FStream(ps*)
    .debug(s => s"Location: $s")
    .evalMap(location => IO.both(handler.loadTokenAsString(location), handler.loadTokenAsBinary(location)))
    .evalTap((v: (String, Vector[Byte])) => IO(scribe.info(s"Loaded TxtLen: ${v._1.length} Bytes: ${v._2.size} ")))
    .compile
    .drain

  /** Bytes are fed from File to ScalaXML */
  def pathToXmlViaScalaXMLBinary: FPipe[IO, FPath, String] = stream =>
    stream
      .evalMap(ScalaXMLParsing.parseFromPathBinary)
      .map(elem => s"Binary: ${ScalaXMLParsing.serialize(elem)}")

  /** Placeholder, this should deal with BOM and charset sniffing from file, defaulting to UTF-8 NO BOM */
  def loadPathAsXmlString(file: FPath) = IO(JFiles.readString(file.toNioPath, Charset.forName("UTF-8")))
  def allParsers(file: FPath)          = {
    for {
      xmlText <- loadPathAsXmlString(file)
      sxmlBin <- ScalaXMLParsing.parseFromPathBinary(file)
      sxmlTxt <- ScalaXMLParsing.parseFromString(xmlText)
      jdkBin  <- JDKParsing.parseFromPathDirectly(file)
      jdkTxt  <- JDKParsing.parserFromString(xmlText)
      _       <- IO.raiseUnless(sxmlTxt.equals(sxmlBin))(Throwable(s"$file ScalaXML Bin And Text Parsing no Match"))
      _        = jdkBin.normalize()
      _        = jdkTxt.normalize()
      // _       <- IO.raiseUnless(jdkBin.equals(jdkTxt))(Throwable(s"$file JDKParsing Bin And Text Parsing no Match"))
      sxmlOut  = ScalaXMLParsing.serialize(sxmlBin)
      jdkOut   = JDKParsing.serialize(jdkBin)
      isEqual  = jdkOut.trim == sxmlOut.trim
      _        = scribe.info(s"\nfile:///$file\n JDK:  $jdkOut \n ScalaXML: $sxmlOut \n")
    } yield (sxmlOut, jdkOut)
  }
}
