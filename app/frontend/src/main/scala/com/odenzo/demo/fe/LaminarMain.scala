package com.odenzo.demo.fe

import cats.effect.*
import cats.effect.implicits.*
import cats.effect.kernel.Outcome
import cats.effect.unsafe.IORuntime
import cats.effect.unsafe.implicits.*
import com.raquo.laminar.api.L.{given, *}

import org.http4s.client.{Client => HTTPClient}

import org.scalajs.dom
import org.scalajs.dom.*
import scribe.Level
import scribe.*
object LaminarMain {

  /** OUr API just need an implicit client, you can make with middleware etc attached */
  given client: HTTPClient[IO] = com.odenzo.demo.httpclient.ClientFactory.asClient

  val sayHi: Observer[Any]          = Observer((a: Any) => scribe.info(s"Will Say Hi"))
  val sayHiXml: Observer[Any]       = Observer((a: Any) => scribe.info(s"Will Say HiXML"))
  val textXMLInput: Var[ByteString] = Var[String]("")

  def main(args: Array[String]): Unit = {
    scribe.info(s"Laminar Main Running")
    val minLevel = Level.Debug

    val elem         = dom.document.querySelector("#laminar")
    val content: Div = div(
      p("This is the Laminar Content...more content soon"),
      p((button("Say Hi", onClick --> sayHi)), p(button("Say Hi XML", onClick --> sayHiXml))),
      div("Future Text Input")
    )
    scribe.info("Existing Main RUN.. back to browser even loop that ...")
    renderOnDomContentLoaded(elem, content)

  }
}
