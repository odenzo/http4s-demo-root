import sbt._

import MyCompileOptions.optV3
import sbt.Keys.libraryDependencies
enablePlugins(JavaAppPackaging)
ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / publishMavenStyle   := true
ThisBuild / bspEnabled          := false
scalaJSUseMainModuleInitializer := true
//Test / scalaJSUseMainModuleInitializer
maintainer                      := "pania.misc@gmail.com"
//Runtime / managedClasspath += (packageBin in previewJVM in Assets).value

// ECMAScript
//scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.ESModule) }
// CommonJS
scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }

Compile / mainClass := Some("com.odenzo.webapp.backend.BEMain")
val javart = "1.11"
scalaVersion              := "3.1.1"
ThisBuild / scalaVersion  := "3.1.1"
organization              := "com.odenzo"
inThisBuild {
  organization := "com.odenzo"

  reStart / mainClass         := Some("com.odenzo.webapp.be.BEMain")
  reStart / javaOptions += "-Xmx2g"
  reStartArgs                 := Seq("-x")
  Test / fork                 := false // ScalaJS can't be forked
  Test / parallelExecution    := false
  Test / logBuffered          := false
  scalacOptions ++= Seq("-release", "11")
  Compile / parallelExecution := false
}
ThisBuild / scalacOptions :=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    // case Some((2, n)) if n >= 13 => optsV213 ++ warningsV213 ++ lintersV213
    case Some((3, _)) => optV3 // ++ lintersV3
    case _            => optV3 // ++ lintersV3
  })

val jsPath = "apps/backend/src/main/resources"

lazy val root = project
  .in(file("."))
  .dependsOn(common.jvm, common.js, frontend.js, backend, httpclient.jvm, httpclient.js)
  .aggregate(common.jvm, common.js, frontend.js, backend, httpclient.jvm, httpclient.js)
  .settings(name := "http4s-demo-root", doc / aggregate := false, publish / skip := true)

lazy val common = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("modules/common"))
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"          %%% "cats-core"              % V.cats,
      "org.typelevel"          %%% "cats-effect"            % V.catsEffect,
      "com.outr"               %%% "scribe"                 % V.scribe,
      "com.lihaoyi"            %%% "pprint"                 % V.pprint,
      "com.odenzo"             %%% "http4s-dom-xml"         % "0.0.2",
      "org.scala-lang.modules" %%% "scala-xml"              % V.scalaXML,
      "org.http4s"             %%% "http4s-circe"           % V.http4s,
      "org.typelevel"          %%% "munit-cats-effect-3"    % V.munitCats % Test,
      "org.latestbit"          %%% "circe-tagged-adt-codec" % "0.10.1"
    )
  )
  .jvmSettings(libraryDependencies ++= Seq())
  .jsSettings()

lazy val httpclient = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Full) in file("modules/httpclient"))
  .dependsOn(common % "compile->compile;test->test")
  .settings(
    name := "httpclient",
    libraryDependencies ++= Seq("org.http4s" %%% "http4s-dsl" % V.http4s, "org.http4s" %%% "http4s-client" % V.http4s)
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.http4s" %%% "http4s-dom"    % V.http4s,
      "io.circe"   %%% "circe-core"    % V.circe,
      "io.circe"   %%% "circe-generic" % V.circe,
      "io.circe"   %%% "circe-pointer" % V.circe,
      "io.circe"   %%% "circe-parser"  % V.circe
    )
  )
  .jvmSettings(libraryDependencies ++= Seq("org.http4s" %% "http4s-ember-client" % V.http4s))

lazy val frontend = (crossProject(JSPlatform).crossType(CrossType.Pure) in file("app/frontend"))
  .dependsOn(common % "compile->compile;test->test", httpclient)
  .jsSettings(
    name                            := "frontend",
    mainClass                       := Some("com.odenzo.investing.fe.LaminarMainIO"),
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq("org.http4s" %%% "http4s-dom" % V.http4s, "com.raquo" %%% "laminar" % V.laminar)
  )

lazy val backend = project
  .in(file("app/backend"))
  .dependsOn(common.jvm % "compile->compile;test->test", httpclient.jvm % "compile->compile;test->test")
  .enablePlugins(JavaAppPackaging)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-dsl"          % V.http4s,
      "org.http4s"    %% "http4s-ember-server" % V.http4s,
      "org.http4s"    %% "http4s-ember-client" % V.http4s, // Was going to spawn a program to use httpclient in jvm, not done yet
      "co.fs2"        %% "fs2-io"              % V.fs2,
      "ch.qos.logback" % "logback-classic"     % V.logback
    )
  )
  .settings(Compile / resourceDirectories ++= Seq(baseDirectory.value / "web"))

lazy val feRun = taskKey[Unit]("Copy JS to Resources and Run WebApp")
feRun := {
  (Compile / compile).value
  (frontend.js / Compile / fastLinkJS).value
  val js              = (frontend.js / Compile / crossTarget).value / "frontend-fastopt" / "main.js"
  val jsmap           = (frontend.js / Compile / crossTarget).value / "frontend-fastopt" / "main.js.map"
  val finalDest: File = (backend / Compile / resourceDirectory).value / "web" / "js"
  IO.copyFile(js, finalDest / "main.js")
  IO.copyFile(jsmap, finalDest / "main.js.map")
  // the reStart on rerun will move from src resources to actual app
  // Must be something that runs the backend from "pre-jarred' directory for even quick turn-around
  // and that will automatically move the recompiled JS in as necessary (even on a per module bases?)

}

addCommandAlias("rerun", "feRun ;backend/reStart")
