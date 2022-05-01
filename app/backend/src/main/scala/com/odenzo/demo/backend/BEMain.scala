package com.odenzo.demo.backend

import cats.*
import cats.data.*
import cats.effect.*
import cats.effect.syntax.all.*
import cats.effect.unsafe.IORuntime
import cats.syntax.all.*
import cats.arrow.FunctionK
import com.comcast.ip4s.*
import com.odenzo.base.{OLogging, ScribeLoggingConfig}
import com.odenzo.demo.backend.routes.{StaticAssets, XMLEchoRoutes, XMLTestRoutes}
import com.odenzo.demo.httpclient.TestResult
import org.http4s.*
import org.http4s.client.{Client, middleware}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import scribe.Level

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object BEMain extends IOApp {

  val builder: EmberClientBuilder[IO]      = EmberClientBuilder.default[IO]
  val asResource: Resource[IO, Client[IO]] = builder.build

  sys.props.addOne("cats.effect.tracing.mode"                -> "cache")
  sys.props.addOne("cats.effect.tracing.buffer.size"         -> "32") // 16 is default ^2
  sys.props.addOne("cats.effect.tracing.exceptions.enhanced" -> "true")

  override def run(args: List[String]): IO[ExitCode] = {
    ScribeLoggingConfig.init(Level.Debug)
    val testor: IO[FiberIO[Unit]] = testHack().start.delayBy(1.seconds)
    server

    // I want to run these two concurrently, in different Threads or so as not to deadlock, async and yielding.

    val result: IO[ExitCode] = Parallel[IO].parProductR(testor.void)(server)
    IO(println(s"Server is background and testor is Parallel... :"))
    result
  }

  def server: IO[ExitCode] = {
    def httpLog(s: String): IO[Unit] = IO(scribe.info(s"JVMServer: $s"))

    val rqrsLogs   = Logger.httpRoutes[IO](true, true, logAction = Some(httpLog(_: String)))
    /* This is aliases somewhere, HTTPApp I think */
    val apiService =
      Router("/" -> StaticAssets.routes, "/xml" -> rqrsLogs(XMLEchoRoutes.routes), "/tests" -> rqrsLogs(XMLTestRoutes.routes)).orNotFound

    val appLogs: HttpApp[IO] => HttpApp[IO] = Logger.httpApp[IO](true, true, logAction = Some(httpLog(_: String)))

    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"127.0.0.1") // Local access only
      .withPort(port"9999")
      .withoutTLS
      .withHttpApp(apiService)
      .build
      .use { _ =>
        scribe.info("USING THE WEBSERVER NOW")
        IO(scribe.info("In IO Using WebServer")) *> IO.never
      }
      .as(ExitCode.Success)
  }

  /** I am going to start the webserver, call this, and then manually test via browser for the frontend */
  def testHack(): IO[Unit] = {
    scribe.info(s"Doing Backend Based Testing")
    asResource.use { client =>
      val clientLoggerMW = middleware.Logger.apply(true, true, logAction = Some((s: String) => IO(scribe.info(s"JVMCLIENT: $s"))))
      val runner         = com.odenzo.demo.httpclient.TestRunner(testCallback, "JDK", clientLoggerMW(client))
      runner.run(testCompleted)

    }
  }

  def testCallback(res: TestResult): IO[Unit] = {
    IO(scribe.info(s"Test Result: ${pprint(res)}"))
  }

  def testCompleted(res: Either[Throwable, Boolean]): Unit = {
    scribe.info(s"Final  Result: ${pprint(res)}")
  }
}
