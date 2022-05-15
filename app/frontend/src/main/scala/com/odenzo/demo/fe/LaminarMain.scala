package com.odenzo.demo.fe

import cats.*
import cats.data.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import cats.effect.kernel.Outcome
import cats.effect.unsafe.IORuntime
import cats.effect.unsafe.implicits.*
import com.odenzo.demo.fe.LaminarMain.lastResult
import com.odenzo.demo.httpclient.*
import com.odenzo.demo.httpclient.XmlUtils.given
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.http4s.client.Client as HTTPClient
import org.scalajs.dom
import org.scalajs.dom.*
import scribe.Level
import scribe.*

import scala.xml.Elem
// import com.odenzo.demo.httpclient.XmlUtils.given // Always gets erased in IJ
import com.odenzo.demo.httpclient.XmlUtils.given
import com.odenzo.demo.httpclient.TestRunner
object LaminarMain {

  val sampleXml = "<ThisIsMyMessageToYou>Hello!</ThisIsMyMessageToYou>"

  /** OUr API just need an implicit client, you can make with middleware etc attached */
  val browserWebClient = com.odenzo.demo.httpclient.ClientFactory.asClient
  val webBridge        = new APIBridge(browserWebClient)
  val testRunner       = new TestRunner(testCB, "Browser", browserWebClient)

  val lastResult: Var[ByteString]   = Var[String]("")
  val textXMLInput: Var[ByteString] = Var[String](sampleXml)

  val actionHandler: Observer[ByteString] = Observer(cmdHandler)

  val testResults: Var[List[TestResult]] = Var[List[TestResult]](List.empty)

  // Try the Laminar smart update slicing thing.
  val testObserver: Signal[List[ReactiveHtmlElement[HTMLElement]]] = testResults.toObservable.map { listOfRes =>
    listOfRes.map {
      case TestErrorResult(uri, mode, expectedPass, notes, error) =>
        tr(td(uri.path), td(expectedPass.toString), td(notes), td(error.map(_.getMessage).getOrElse("")))
      case TestSuccessResult(uri, mode, expectedPass, notes)      =>
        tr(td(uri.path), td(expectedPass.toString))

    }
  }

  /** Individual Test Finished */
  def testCB(res: TestResult): IO[Unit] = {
    testResults.update(_.prepended(res)).pure
  }

  /** ALl the tests are completed */
  def allTestsDoneCB(res: Either[Throwable, Boolean]) = IO.unit

  /** Normally op is an enum.... */
  def cmdHandler(op: String): Unit = {
    val desc = s"Handling Cmd: $op \t\n"
    scribe.info(desc)

    val action = op match {
      case "SayHi"    => webBridge.sayHello.map(t => lastResult.set(desc + t)).void
      case "SayHiXml" => webBridge.sayHelloWithXml.map(el => lastResult.set(desc + el.show)).void
      case "XmlText"  => webBridge.echoAsXml(textXMLInput.now()).map(el => lastResult.set(desc + el.show)).void
      case "XmlElem"  =>
        IO(com.odenzo.xxml.XXMLParser.parse(textXMLInput.now()))
          .flatMap(webBridge.postXmlAndEcho)
          .map((el: xml.Elem) => lastResult.set(desc + el.show))

      // Crude display of just checking no failures now, should diff the scala.xml.Elem produced on server side with one on produced client
      // side
      case "ValidTests" => testRunner.run(allTestsDoneCB)

      case "LoadAllTests" =>
        val mode = TestXmlType.TXT_SNIFFED
        webBridge.getTestsInDir(XmlLocation("valid")).flatMap { files =>
          files.traverse { location =>
            webBridge
              .getXml(location, mode)
              .redeem(
                err => TestErrorResult(location, mode, true, err.getMessage, err.some),
                elem => TestSuccessResult(location, mode, true, XmlUtils.serialize(elem))
              )

          }
        }

      case other => IO.delay(lastResult.set(s"The Command [$other] was not configured."))
    }

    action.unsafeRunAsyncOutcome {
      case Outcome.Succeeded(fa) => ()
      case Outcome.Errored(e)    => lastResult.set(s"$op Error: ${e.getMessage}")
      case Outcome.Canceled()    => lastResult.set(s"$op Cancelled")
    }
  }

  def main(args: Array[String]): Unit = {
    scribe.info(s"Laminar Main Running")
    val minLevel = Level.Debug

    val elem         = dom.document.querySelector("#laminar")
    val content: Div = div(
      p("This is the Laminar Content...more content soon"),
      p(button("Say Hi", onClick.mapTo("SayHi") --> actionHandler), p(button("Say Hi XML", onClick.mapTo("SayHiXml") --> actionHandler))),
      div(
        textArea(
          width       := "80em",
          height      := "10em",
          placeholder := "Enter XML with 1 Root Element",
          sampleXml,
          onChange.mapToValue --> textXMLInput
        ),
        p(
          button("Submit As Text", onClick.mapTo("XmlText") --> actionHandler),
          button("Submit As XML", onClick.mapTo("XmlElem") --> actionHandler)
        ),
        p("Both submit as application/xml, but submitting as XML will parse to XML on client then serialize to text via EntityEncoder")
      ),
      hr(),
      div(p("Last Text Result:"), pre(child <-- lastResult.toObservable.map(t => p(t)))),
      hr(),
      h3("Run Test Suite"),
      button("Run Valid Tests", onClick.mapTo("ValidTests") --> actionHandler),
      div(table(thead(tr(th("Location"), th("Check1"), th("Check3"), th("Latest Note:"))), tbody(children <-- testObserver)))
    )

    scribe.info("Existing Main RUN.. back to browser even loop that ...")
    renderOnDomContentLoaded(elem, content)
  }

}
