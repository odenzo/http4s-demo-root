package com.odenzo.demo.backend

import org.w3c.dom.Document
import org.xml.sax.InputSource

import java.io.{InputStream, StringReader, StringWriter}
import java.net.URL
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

  val builderFactory: DocumentBuilderFactory = {
    val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance

    dbf.setNamespaceAware(true)
    dbf.setValidating(false)
    dbf.setCoalescing(false)
    dbf.setExpandEntityReferences(false)
    dbf.setIgnoringComments(false)
    dbf.setIgnoringElementContentWhitespace(false)
    dbf.setXIncludeAware(false)

    dbf
  }

  def parse(uriAsString: String): Document = {
    println("JVM Parsing")
    val db            = builderFactory.newDocumentBuilder
    val doc: Document = db.parse(uriAsString)
    return doc
  }

  /** The problem with scala.xml.XML.load() is it gives us root, but throws away DOCTYPE (and XML PI) This is a pure function, returning a
    * String and multithread safe. Its kinda worthless. Will always be standalone, and in whatever encoding you convert to on "export" to
    * http4s or nio etc.
    */
  def serialize(doc: Document): String = {
    // /doc.normalizeDocument()

    val transformer = serializerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer.setOutputProperty(OutputKeys.STANDALONE, "yes")

    val writer    = new StringWriter();
    transformer.transform(new DOMSource(doc), new StreamResult(writer));
    val xmlString = writer.getBuffer.toString;

    xmlString

  }

}
