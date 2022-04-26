package com.odenzo.demo.httpclient

import cats.effect.*
import cats.effect.syntax.all.*
import cats.*
import cats.data.*
import cats.implicits.*

import scala.xml.{Elem, Node}

object XmlUtils {

  def showXmlToText(el: Elem): String = scala.xml.Utility.serialize(el).toString()

  given showXml: Show[Elem] = Show.show[Elem](showXmlToText)

}
