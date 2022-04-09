val Http4sVersion = "0.23.11"
val CirceVersion = "0.14.1"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.10"
val MunitCatsEffectVersion = "1.0.7"
val tsecV = "0.4.0"
val pureConfigVersion = "0.17.1"
val testcontainersScalaVersion = "0.40.3"

organization := "dev.sampalmer"
name := "scrapbook"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.13.8"

val fakeKey = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGAevf7LtP7j+JQL+rPZMy+uh+3yVA4kaaDHDycw6gPelouresqHPRR7vVinjmgWt/TVuMBjEgTg5r7x073aSFZHlibUSv8oi9fj5F7GF5Jipm3SKfHwdS3fwZdWUB/kqKUgASBXPiUQjI2LKYSUkSnrjT0YqYOr7iD+MrvuQCGArsCAwEAAQKBgDSxXKY1Wh/O15Og59wGzFfPTZ7rTEJFevs3kelZc8B3Mnd+RN6BZzahWe/5O5iVPueFgN1O6WXEnM1MLkKTcq8xHwB4SyyJYXKCuD2UsxutXHtN59HkBlpzsIyXehIGr3p/QAWHpSdxOGcqf1nXlAluS32NM6kk3J/Hx/MuB8NJAkEAt1ZY2jdlkZfLmQJ+eJZbJ9S7chdqednUhyjo68I3yJOt7im6/ZVo5CumT51M3BpBZJloklW9ri2YA7tKgDSQVQJBAKu0jTJQoOmtzfpu6tsE3m/xHNXVrnn1MIt73i+KgI6QqGKCbv+lv/fxho6ydXFSrro7Yf3CrZNgvKTShNEmFs8CQGSnFyjWeQCxoaljYSO7CFiZxj8g8+fp23BI2Xd1rUKFMVwrtOk7edaq0CetaGD+WSBtyKduQzC4/1gtHv273fECQQCiDvwBFCZ7x0YUauGRLAxBfUFo9ZACnY9e5t8y4bcqV5AFwfO/qBICk1wbjIXaGl69eNSo7DhlVDVYHzWqLRfzAkEAoJHJKvOffokpD+BOVV3/bT6hOcUgXMdgDDTX8JoUbsgLFP0t5Ao49CLVHfj1kRyuAlyiBkpuT29MIwXuYJtk3w=="


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
      "is.cir" %% "ciris" % "2.3.2",
      "org.pac4j" %% "http4s-pac4j" % "4.0.0",
      "org.pac4j" % "pac4j-oidc" % "5.4.2",
      "org.mindrot" % "jbcrypt" % "0.4",
      "io.chrisdavenport" %% "fuuid" % "0.8.0-M2",
      "dev.sampalmer" %% "aws-presigned-scala" % "0.0.2",
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC1",
      "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC1",
      "org.mockito" %% "mockito-scala" % "1.16.39" % Test,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % testcontainersScalaVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % testcontainersScalaVersion % Test,
      "com.dimafeng" %% "testcontainers-scala-munit" % testcontainersScalaVersion % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1"),
    testFrameworks += new TestFramework("munit.Framework")
  ).enablePlugins(SbtTwirl)

Test / fork := true
Test / envVars := Map("AWS_ACCESS_KEY_ID" -> "test", "AWS_SECRET_ACCESS_KEY" -> "test", "PRIVATE_KEY" -> fakeKey, "KEY_ID" -> "test")