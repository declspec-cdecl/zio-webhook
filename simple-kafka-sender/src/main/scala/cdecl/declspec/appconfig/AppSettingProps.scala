package cdecl.declspec.appconfig

import cats.kernel.Monoid
import cats.implicits._

case class AppSettingProps(bootStrapServer: Option[String] = None,
                           topic: Option[String] = None,
                           sendingIntervalMillis: Option[Int] = None
                          ) {
  def toKafkaCommonSettings: Option[KafkaCommonSettings] =
    (bootStrapServer, topic).mapN((bootstrap, topic) => KafkaCommonSettings(List(bootstrap), topic))


  def toAppSettings: Option[AppSettings] =
    (toKafkaCommonSettings, sendingIntervalMillis).mapN(AppSettings.apply)
}

object AppSettingProps {

  implicit val propsMonoid = new Monoid[AppSettingProps] {
    override def empty: AppSettingProps = AppSettingProps()

    override def combine(x: AppSettingProps, y: AppSettingProps): AppSettingProps =
      AppSettingProps(
        Monoid[Option[String]].combine(x.bootStrapServer, y.bootStrapServer),
        Monoid[Option[String]].combine(x.topic, y.topic),
        Monoid[Option[Int]].combine(x.sendingIntervalMillis, y.sendingIntervalMillis),
      )
  }

}
