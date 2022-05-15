package com.odenzo.demo.httpclient

import cats.effect.*
import cats.effect.syntax.all.*
import cats.*
import cats.data.*
import cats.implicits.*

// This *is* present in scalajs
import java.io.StringWriter
import scala.xml.{Elem, Node}
import scala.xml.dtd.DocType

object XmlUtils {

  /** The problem with scala.xml.XML.load() is it gives us root, but throws away DOCTYPE (and XML PI) This is a pure function, returning a
    * String and multithread safe. Its kinda worthless. Will always be standalone, and in whatever encoding you convert to on "export" to
    * http4s or nio etc.
    */
  def serialize(e: Elem): String =
    val writer           = new StringWriter(1024)
    val decl             = false
    val docType: DocType = null
    scala.xml.XML.write(writer, e, "UTF-8", decl, docType)
    writer.flush()
    writer.close()
    writer.toString

  given showXml: Show[Elem] = Show.show[Elem](serialize)

}
