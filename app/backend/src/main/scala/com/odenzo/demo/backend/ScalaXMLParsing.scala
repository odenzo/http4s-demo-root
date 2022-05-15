package com.odenzo.demo.backend

import cats.syntax.all.*
import cats.effect.IO
import fs2.io.file.Path as FPath
import org.w3c.dom.Document
import org.xml.sax.InputSource

import java.io.{ByteArrayInputStream, FileInputStream, StringReader, StringWriter}
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import scala.xml.dtd.DocType

/** */
object ScalaXMLParsing {

  import javax.xml.parsers.SAXParserFactory
  import scala.xml.factory.XMLLoader
  import scala.xml.{Elem, XML}

  protected val factory: SAXParserFactory = SAXParserFactory.newInstance()
  factory.setFeature("http://xml.org/sax/features/validation", true)
  factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
  factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
  factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

  // This creates a new object in withSAXParser. This should be a class to be safe since I am
  // am pretty sure a SAX Parser is not multithread safe.
  private def customXML: XMLLoader[Elem] = XML.withSAXParser {
    factory.newSAXParser()
  }

  def parseFromPathBinary(path: FPath): IO[Elem] = IO {
    // val channel = Files.newByteChannel(jpath)
    val bytesIn: Array[Byte]      = Files.readAllBytes(path.toNioPath)
    val bis: ByteArrayInputStream = new ByteArrayInputStream(bytesIn)
    // val fis                       = new FileInputStream(path.toNioPath.toFile)
    val in                        = new InputSource(bis)
    scribe.info(s"ScalaXML Parsing ${path} as URL: ")
    in
  } >>= parse

  def parseFromString(xmlText: String): IO[Elem] = IO {
    customXML.loadString(xmlText)
  }

  /** Directly parses from File, with ScalaXML handling all file encoding matters */
  def parseFromPath(fileAbs: FPath): IO[Elem] = IO {
    customXML.load(fileAbs.toNioPath.toUri.toURL) match {
      case elem: scala.xml.Elem => elem
      // case other                => throw Throwable(s"Expected Parsed elem got ${other.getClass}: $other")
    }
  }

  def parse(in: InputSource): IO[Elem] = IO {
    customXML.load(in)
  }

  /** The problem with scala.xml.XML.load() is it gives us root, but throws away DOCTYPE (and XML PI) This is a pure function, returning a
    * String and multithread safe. Its kinda worthless. Will always be standalone, and in whatever encoding you convert to on "export" to
    * http4s or nio etc.
    */
  def serialize(e: Elem): String =
    val writer           = new StringWriter(1024)
    val decl             = false
    val docType: DocType = null
    scala.xml.XML.write(writer, e, "UTF-8", false, docType)
    writer.flush()
    writer.close()
    writer.toString
}
