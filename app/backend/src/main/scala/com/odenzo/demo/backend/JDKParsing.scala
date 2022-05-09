package com.odenzo.demo.backend

import cats.effect.IO
import cats.syntax.all.*
import fs2.io.file.Path
import org.w3c.dom.Document
import org.xml.sax.InputSource

import java.io.{InputStream, StringReader, StringWriter}
import java.net.URL
import java.nio.charset.Charset
import javax.xml.parsers.SAXParser
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.{OutputKeys, TransformerFactory}
import scala.xml.dtd.DocType

/** */
object JDKParsing {
  import javax.xml.parsers.DocumentBuilder
  import java.io.File
  import javax.xml.parsers.SAXParserFactory
  import scala.xml.factory.XMLLoader
  import scala.xml.{Elem, XML}

  import javax.xml.parsers.DocumentBuilder;
  import javax.xml.parsers.DocumentBuilderFactory;
  import javax.xml.parsers.DocumentBuilderFactory

  val serializerFactory: TransformerFactory = {
    val tf: TransformerFactory = TransformerFactory.newInstance()
    println(s"TranfsormerFactory Instance: $tf   (${tf.getClass})")
    tf
  }

  /** Want to fine tune these to match DOMParser and be compatable in x-platform scala.xml.Elem result (Same for the serializer) */
  val builderFactory: DocumentBuilderFactory = {
    val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance

    dbf.setNamespaceAware(true)
    dbf.setValidating(false)
    dbf.setCoalescing(false)
    dbf.setExpandEntityReferences(true)
    dbf.setIgnoringComments(true)
    dbf.setIgnoringElementContentWhitespace(false)
    dbf.setXIncludeAware(false)

    dbf
  }

  def parseFromPathDirectly(path: Path): IO[Document] = IO {
    val uriStr = path.toNioPath.toUri.toString
    val in     = new InputSource(uriStr)
    println(s"JVM Parsing ${path} as URL: ")
    in
  } >>= parse

  /** This assumes that the xmlText is correctly encoded and we convert to UTF-8 Bytes. If XML prolog has a different encoding this may fail
    * (?)
    */
  def parserFromString(xmlText: String): IO[Document] = IO {
    val db                        = builderFactory.newDocumentBuilder
    import java.io.ByteArrayInputStream
    val sr: StringReader          = new StringReader(xmlText)
    val bis: ByteArrayInputStream = new ByteArrayInputStream(xmlText.getBytes(Charset.forName("UTF-8")))
    new InputSource(sr)
  } >>= parse

  def parse(source: InputSource): IO[Document] = IO {
    val db            = builderFactory.newDocumentBuilder
    val doc: Document = db.parse(source)
    doc
  }

  /** The problem with scala.xml.XML.load() is it gives us root, but throws away DOCTYPE (and XML PI) This is a pure function, returning a
    * String and multithread safe. Its kinda worthless. Will always be standalone, and in whatever encoding you convert to on "export" to
    * http4s or nio etc.
    */
  def serialize(doc: Document): String = {
    // /doc.normalizeDocument()

    val transformer = serializerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    // transformer.setOutputProperty(OutputKeys.STANDALONE, "yes")

    val writer    = new StringWriter();
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    val xmlString = writer.getBuffer.toString;

    xmlString

  }

}
