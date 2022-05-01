package com.odenzo.demo.backend

import java.io.StringWriter
import java.net.URL
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

  /** Creates a new loader and parses. This is thread safe */
  def parse(url: URL): Elem = { customXML.load(url) }

  /** Creates a new loader and parses. This is thread safe */
  def parse(s: String): Elem = {
    println("JVM Parsing")
    customXML.loadString(s)
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
