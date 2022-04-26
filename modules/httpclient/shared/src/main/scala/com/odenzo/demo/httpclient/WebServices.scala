package com.odenzo.demo.httpclient

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.implicits.*
import com.odenzo.demo.httpclient.XmlUtils.showXmlToText
import org.http4s.client.Client
import com.odenzo.xxml.*

import scala.xml.Elem

/** Demonstrating POSTing XML and also receiving XML response */
class WebServices(client: Client[IO]) {

  def sayHi: IO[String] = client.expect[String](API.ping)

  def sayHiXml: IO[Elem] = client.expect[Elem](API.getHelloInXML)

  def echoXml(el: Elem): IO[Elem] = client.expect[Elem](API.postXml(el))

  def echoXmlText(xmlText: String): IO[Elem] = client.expect[Elem](API.postXmlText(xmlText))
}
