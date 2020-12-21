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
    name := "webhook-service",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir"  %% "tapir-zio"                      % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-zio-http4s-server"        % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-json-circe"               % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-swagger-ui-http4s"        % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-redoc-http4s"             % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-openapi-docs"             % tapirVersion,
      "com.softwaremill.sttp.tapir"  %% "tapir-openapi-circe-yaml"       % tapirVersion,
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
      "dev.zio"                      %% "zio-kafka"                      % "0.13.0",
      "org.typelevel"                %% "cats-core"                      % "2.3.0",
      "com.softwaremill.sttp.client" %% "core"                           % "2.2.9",
      "com.softwaremill.sttp.client" %% "async-http-client-backend-zio"  % "2.2.9",
      "com.softwaremill.sttp.client" %% "circe"                          % "2.2.9",
      "org.tpolecat"                 %% "doobie-hikari"                  % "0.9.0",
      "org.tpolecat"                 %% "doobie-core"                    % "0.9.0",
      "org.tpolecat"                 %% "doobie-postgres"                % "0.9.0",
      "org.flywaydb"                 %  "flyway-core"                    % "6.3.1",
      "dev.zio"                      %% "zio-test"                       % zioVersion % "test",
      "dev.zio"                      %% "zio-test-sbt"                   % zioVersion % "test",
      "com.fasterxml.jackson.core"   % "jackson-databind"                % "2.10.2"
    ),
  )
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

dockerExposedPorts ++= Seq(9000)
packageName in Docker := "webhook-service"
version in Docker := "0.1"
dockerBaseImage := "adoptopenjdk/openjdk8:jre8u275-b01"

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.11.2" cross CrossVersion.full)
addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.1")
resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sbtPluginRepo("releases")