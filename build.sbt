import sbt._

import MyCompileOptions.optV3
import sbt.Keys.libraryDependencies

import scala.Seq
ThisBuild / resolvers += Resolver.mavenLocal
ThisBuild / publishMavenStyle   := true
ThisBuild / bspEnabled          := false
scalaJSUseMainModuleInitializer := true

ThisBuild / scalaJSLinkerConfig ~= {
  _.withModuleKind(ModuleKind.ESModule)
}

val javart                      = "1.11"
lazy val supportedScalaVersions = List("3.1.1")
ThisBuild / scalaVersion  := "3.1.1"
inThisBuild {

  organization                := "com.odenzo"
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
  .aggregate(common.jvm, common.js, frontend.js, backend, httpclient.jvm, httpclient.js)
  .settings(name := "http4s-demo-root", crossScalaVersions := supportedScalaVersions, doc / aggregate := false)

lazy val common = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("modules/common"))
  .settings(
    libraryDependencies ++=
      Seq(XLib.cats.value, XLib.catsEffect.value, XLib.pprint.value, XLib.scribe.value, XLib.munit.value, XLib.munitCats.value),
    libraryDependencies ++= Seq("com.odenzo" %%% "http4s-dom-xml" % "0.0.1", "org.scala-lang.modules" %%% "scala-xml" % "2.1.0")
  )
  .jvmSettings(libraryDependencies ++= Seq())
  .jsSettings()

lazy val httpclient = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Full) in file("modules/httpclient"))
  .dependsOn(common % "compile->compile;test->test")
  .settings(
    name := "httpclient",
    libraryDependencies ++= Seq("org.http4s" %%% "http4s-dsl" % V.http4s, "org.http4s" %%% "http4s-client" % V.http4s)
  )
  .jsSettings(libraryDependencies ++= Seq("org.http4s" %%% "http4s-dom" % V.http4s))
  .jvmSettings(libraryDependencies ++= Seq("org.http4s" %% "http4s-ember-client" % V.http4s))

lazy val frontend = (crossProject(JSPlatform).crossType(CrossType.Pure) in (file("app/frontend")))
  .dependsOn(common % "compile->compile;test->test", httpclient)
  .jsSettings(
    name                            := "frontend",
    mainClass                       := Some("com.odenzo.investing.fe.LaminarMainIO"),
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(XLib.http4sDomClient.value),
    libraryDependencies ++= Seq(JSLibs.laminar.value, JSLibs.laminarExtCore.value)
  )

lazy val backend = project
  .in(file("app/backend"))
  .dependsOn(common.jvm % "compile->compile;test->test", httpclient.jvm % "compile->compile;test->test")
  .settings(libraryDependencies ++= Libs.testing ++ Libs.scribeSLF4J ++ Libs.http4s)
  .settings(Compile / resourceDirectories ++= Seq(baseDirectory.value / "web"))

// This will recompile everything (changed) and the link the Javascript into Backend resources directory
// Not sure under which circumatances we need to move to backend/target or actually repack the JAR
// Have to read how revolver actually works.
// Odd, that given this seems a great/standard use-case it isn't well documented
// how to set this up with quick dev cycle, then a nice production version
lazy val feRun = taskKey[Unit]("Copy JS to Resources and Run WebApp")
feRun := {
  (Compile / compile).value
  (frontend.js / Compile / fastLinkJS).value
  val js              = (frontend.js / Compile / crossTarget).value / "frontend-fastopt" / "main.js"
  val jsmap           = (frontend.js / Compile / crossTarget).value / "frontend-fastopt" / "main.js.map"
  val finalDest: File = (backend / Compile / resourceDirectory).value / "web" / "js"
  IO.copyFile(js, finalDest / "main.js")
  IO.copyFile(jsmap, finalDest / "main.js.map")

}

addCommandAlias("rerun", "feRun ;backend/reStart")
