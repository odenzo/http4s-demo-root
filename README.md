# http4s-dom-xml demo

Demonstration app for the http4s extension to use XML EntityEncoder and EntityDecoder in ScalaJS Client where the client
is x-compiled for JVM and JS from same source code.

## `com.odenzo.demo.httpclient` illustrates
1. sending XML as text, to be interpreted by server an application/xml
2. POSTing a scala-xml document represented by an `scala.xml.Elem`
3. Receiving a response XML document, and decoding into `scala.xml.Elem`

Note:

- All this code is JS/JVM compatable by import `com.odenzo.xxml._` which will import EntityDecoder and EntityEncoder.
Either the ScalaJS one provided, or if on JVM importing the http4s.core suppled XML Encoder and Decoder.

- The code requires a Client[IO] which is constructed *differently* in ScalaJS (using FetchClient) vs JVM (using Ember or Blaze Client).
Both produce a Client[IO] though to the core logic part can remain x-platform.

- The majority of scala-xml functions are cross-platform, just not the parsing, so same code on JVM/JS for dealing with responses etc.


## `com.odenzo.demo.fe.LaminarMain` demonstrates **client side only** usage 

This includes creating a Client[IO] that is usable from Javascript and a little hack to convert text XML to scala.xml.Elem in the browser.


# Environment / Running

This is a Scala 3.1 ScalaJS 1.10 application.

rerun will compile the code, bundle the Javascript and launch a WebServer. To access the webserver open  http://127.0.0.1:9999/ in your 
browser. The app only connects to 127.0.0.1 and the WebServer only serves on 127.0.0.1 (not 0.0.0.0)

It uses scribe for logging on backend and front-end, see the Javascript logs in your browser.


NOTE: This is really only designed to be run from an IDE, or at least not with TestData jar'ed up.

