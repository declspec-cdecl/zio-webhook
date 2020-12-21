package cdecl.declspec

import zio.test._
import zio.test.Assertion._
import cdecl.declspec.appconfig.AppSettings

object AppSettingsParsingSpec extends DefaultRunnableSpec {
  val rightConfig = Map(
    "retryseconds" -> "20",
    "bootstrapserver" -> "0.0.0.0:9093",
    "dbuser" -> "declspec",
    "dbpassword" -> "pswd",
    "dburl" -> "jdbc:postgresql://0.0.0.0:5445/webhooksdb",
    "timeoutmillis" -> "2500",
    "topic" -> "messages"
  )

  val positiveIntProps = List("retryseconds", "timeoutmillis")

  def spec = suite("AppSettingsParsingSpec")(
    test("successful config parsing") {
      assert(AppSettings.fromEnvs(rightConfig))(isRight)
    },
    test("field absence") {
      assert(rightConfig.keys.map(s => AppSettings.fromEnvs(rightConfig.removed(s))))(forall(isLeft))
    },
    test("empty fields") {
      assert(rightConfig.keys.map(s => AppSettings.fromEnvs(rightConfig.updated(s, ""))))(forall(isLeft))
    },
    test("negative props") {
      assert(positiveIntProps.map(s => AppSettings.fromEnvs(rightConfig.updated(s, "-1"))))(forall(isLeft))
    }
  )

}
