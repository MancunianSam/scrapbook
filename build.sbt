val Http4sVersion = "0.23.11"
val CirceVersion = "0.14.1"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.10"
val MunitCatsEffectVersion = "1.0.7"
val tsecV = "0.4.0"
val pureConfigVersion = "0.17.1"

organization := "dev.sampalmer"
name := "scrapbook"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.13.8"

lazy val scalaJs = (project in file("scala-js"))
  .settings(
    scalaJSUseMainModuleInitializer := true
  ).enablePlugins(ScalaJSPlugin)

lazy val root = (project in file("."))
  .settings(

    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"      %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "org.http4s"      %% "http4s-twirl"        % Http4sVersion,
      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion         % Runtime,
      "org.scalameta"   %% "svm-subs"            % "20.2.0",
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
      "io.chrisdavenport" %% "fuuid" % "0.8.0-M2",
      "dev.sampalmer" %% "aws-presigned-scala" % "0.1.0-SNAPSHOT",
      "io.github.jmcardon" %% "tsec-http4s" % tsecV
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  ).enablePlugins(SbtTwirl)
