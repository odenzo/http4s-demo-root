package com.odenzo.demo.backend.routes

import cats.effect._
import org.http4s.dsl.io._
import org.http4s.{StaticFile, HttpRoutes}
import org.http4s.EntityEncoder._

object StaticAssets:

  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {

    case request @ GET -> Root => // Wonder if we can Eval.once this to cache
      IO(scribe.warn(s"Getting Root Resource $request")) *>
        StaticFile.fromResource[IO]("/web/index.html", Some(request)).getOrElseF(NotFound("Could not find the index.html Static File"))

    case request @ GET -> Root / "index.html" => // Wonder if we can Eval.once this to cache
      IO(scribe.warn(s"Getting / index.html  Resource $request")) *>
        StaticFile.fromResource[IO]("/web/index.html", Some(request)).getOrElseF(NotFound("Could not find the index.html Static File"))

    case request @ GET -> Root / "favicon" =>
      IO(scribe.warn(s"Getting / index.html  Resource $request")) *>
        StaticFile.fromResource[IO]("/index.html", Some(request)).getOrElseF(NotFound("No FavIcon"))

    case request @ GET -> Root / "css" / path if staticFileAllowed(path) =>
      StaticFile.fromResource("/web/css/" + path, Some(request)).getOrElseF(NotFound())

    case request @ GET -> Root / "js" / path if staticFileAllowed(path) =>
      StaticFile.fromResource("/web/js/" + path, Some(request)).getOrElseF(NotFound())

    case request @ GET -> Root / "media" / path if staticFileAllowed(path) =>
      StaticFile.fromResource("/web/media/" + path, Some(request)).getOrElseF(NotFound())

  }

  private def staticFileAllowed(path: String) = List(".gif", ".js", ".css", ".map", ".html", ".webm", ".js.map").exists(path.endsWith)
