package com.odenzo.demo.backend

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import cats.arrow.FunctionK
import com.comcast.ip4s.*

import com.odenzo.base.OLogging
import com.odenzo.demo.backend.routes.{StaticAssets, XMLEchoRoutes}
import org.http4s.*
import org.http4s.client.{Client, middleware}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.middleware.Logger

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** The main purpose of this web server is *just* to serve our front-end client.
  *   - For e-trade it will start its on oauth callback server.
  *   - For FlexQuery we need no login or server (front-end or back-end could do)
  *   - For IBKR TWS we need TWS ow TWS Gateway manual login and use Java so back-end only
  *   - For IBKR Portal we connect directly to IB Gateway 10.x
  *
  * E-Trade is kind of the pain in the ass since we need to keep the login around alot, but don't want to start it unless needed. Even
  * stuffed into middleware means we need to client around.
  */
object BEMain extends IOApp {

  sys.props.addOne("cats.effect.tracing.mode"                -> "cache")
  sys.props.addOne("cats.effect.tracing.buffer.size"         -> "32") // 16 is default ^2
  sys.props.addOne("cats.effect.tracing.exceptions.enhanced" -> "true")

  override def run(args: List[String]): IO[ExitCode] = {

    /* This is aliases somewhere, HTTPApp I think */
    val apiService =
      Router("/" -> StaticAssets.routes, "/xml" -> XMLEchoRoutes.routes).orNotFound

    val fk: FunctionK[IO, IO] = new FunctionK[IO, IO] {
      def apply[A](l: IO[A]): IO[A] = l
    }
    //    val logMW                 =
    //      org.http4s.server.middleware
    //        .Logger[IO, IO](logHeaders = true, fk = fk, logBody = false, logAction = Some(loggingFn))(apiService)

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"127.0.0.1") // Local access only
      .withPort(port"9999")
      .withoutTLS
      .withHttpApp(apiService)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

}
