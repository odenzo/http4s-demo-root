package com.odenzo.demo.httpclient

import cats.effect.Resource
import org.http4s.client.Client

/** In ScalaJS land we create a Fetch Client from http4s-dom */
object ClientFactory {
  import cats.effect.IO

  val asResource: Resource[IO, Client[IO]] = org.http4s.dom.FetchClientBuilder[IO].withDefaultCache.resource
  val asClient: Client[IO]                 = org.http4s.dom.FetchClientBuilder[IO].create
}
