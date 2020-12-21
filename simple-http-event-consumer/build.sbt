import sbt.Keys.libraryDependencies

val circeVersion   = "0.14.0-M1"
val tofuVersion    = "0.8.0"
val logbackVersion = "1.2.3"
val tschemaVersion = "0.12.7"
val derevoVersion  = "0.11.5"
val zioCatsVersion = "2.2.0.1"
val zioVersion     = "1.0.3"
val tapirVersion   = "0.17.0-M11"
val root = (project in file("."))
  .settings(
    scalaVersion := "2.13.4",
    scalacOptions := Seq(
      "-feature",
      "-deprecation",
      "-explaintypes",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:existentials",
      "-Yrangepos",
      "-Xlint:-infer-any,_",
      "-Ywarn-value-discard",
      "-Ywarn-numeric-widen",
      "-Ywarn-extra-implicit",
      "-Ywarn-unused:_",
      "-Ymacro-annotations"
    ) ++ (if (isSnapshot.value) Seq.empty
    else
      Seq(
        "-opt:l:inline"
      )),
    version := "0.1",
    name := "simple-http-event-consumer",
    libraryDependencies ++= Seq(
      "ru.tinkoff"                   %% "tofu"                           % tofuVersion,
      "ru.tinkoff"                   %% "tofu-logging"                   % tofuVersion,
      "ru.tinkoff"                   %% "tofu-zio-interop"               % tofuVersion,
      "ru.tinkoff"                   %% "tofu-logging-derivation"        % tofuVersion,
      "io.circe"                     %% "circe-core"                     % circeVersion,
      "io.circe"                     %% "circe-generic"                  % circeVersion,
      "io.circe"                     %% "circe-parser"                   % circeVersion,
      "io.circe"                     %% "circe-generic-extras"           % "0.13.0",
      "ch.qos.logback"               % "logback-classic"                 % logbackVersion,
      "dev.zio"                      %% "zio"                            % zioVersion,
      "dev.zio"                      %% "zio-streams"                    % zioVersion,
      "dev.zio"                      %% "zio-interop-cats"               % zioCatsVersion,
      "org.typelevel"                %% "cats-core"                      % "2.3.0",
      "org.http4s"                   %% "http4s-server"                  % "0.21.13",
      "com.softwaremill.sttp.tapir"  %% "tapir-zio"                      % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-zio-http4s-server"        % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-json-circe"               % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-swagger-ui-http4s"        % tapirVersion
    ),
  )

dockerExposedPorts ++= Seq(9000)
packageName in Docker := "simple-http-event-consumer"
version in Docker := "0.1"
dockerBaseImage := "adoptopenjdk/openjdk8:jre8u275-b01"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.2" cross CrossVersion.full)
addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sbtPluginRepo("releases")
