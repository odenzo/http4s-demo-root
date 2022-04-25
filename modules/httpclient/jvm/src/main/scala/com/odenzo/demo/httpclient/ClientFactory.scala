package com.odenzo.demo.httpclient

import cats.effect.Resource

/** In JVM land we create an Ember Client */
object ClientFactory {
  import org.http4s.ember.client.*
  import org.http4s.client.*
  import cats.effect.IO

  val builder: EmberClientBuilder[IO] = EmberClientBuilder.default[IO]

  val asResource: Resource[IO, Client[IO]] = builder.build

}
