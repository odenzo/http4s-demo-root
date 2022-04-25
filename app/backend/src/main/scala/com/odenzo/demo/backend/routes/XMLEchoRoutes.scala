package com.odenzo.demo.backend.routes

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.kernel.Outcome
import cats.effect.syntax.all.*
import cats.syntax.all.*
import com.odenzo.base.OPrint.oprint
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

object XMLEchoRoutes extends RouteUtils:

  import org.http4s.EntityEncoder.*
  import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
  import cats.effect.Ref.*
  import cats.effect.unsafe.implicits.global
  import scala.xml.*
  import org.http4s.scalaxml._
  val routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case rq @ POST -> Root / "echo" =>
      for {
        xml  <- rq.as[Elem]
        reply = <YouSentMe>xml</YouSentMe>
        rs   <- Ok(reply)
      } yield rs

    case GET -> Root / "ping" => // Plain Text Response
      Ok("HELLO!")

    case GET -> Root / "xmlPing" => // XML Response
      val reply = <greeting>Hello!</greeting>
      Ok(reply)

  }
