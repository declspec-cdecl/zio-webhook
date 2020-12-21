package cdecl.declspec.appconfig

import cats.kernel.Monoid
import cats.implicits._

case class AppSettingProps(bootStrapServer: Option[String] = None,
                           topic: Option[String] = None,
                           retrySeconds: Option[Int] = None,
                           timeoutMillis: Option[Int] = None,
                           dbUrl: Option[String] = None,
                           dbUser: Option[String] = None,
                           dbPassword: Option[String] = None
                          ) {
  def toKafkaCommonSettings: Option[KafkaCommonSettings] =
    (bootStrapServer, topic).mapN((bootstrap, topic) => KafkaCommonSettings(List(bootstrap), topic))

  def toDbConfig: Option[DbConfig] =
    (dbUrl, dbUser, dbPassword).mapN(DbConfig.apply)

  def toAppSettings: Option[AppSettings] =
    (toKafkaCommonSettings, retrySeconds, timeoutMillis, toDbConfig).mapN(AppSettings.apply)
}

object AppSettingProps {

  implicit val propsMonoid = new Monoid[AppSettingProps] {
    override def empty: AppSettingProps = AppSettingProps()

    override def combine(x: AppSettingProps, y: AppSettingProps): AppSettingProps =
      AppSettingProps(
        Monoid[Option[String]].combine(x.bootStrapServer, y.bootStrapServer),
        Monoid[Option[String]].combine(x.topic, y.topic),
        Monoid[Option[Int]].combine(x.retrySeconds, y.retrySeconds),
        Monoid[Option[Int]].combine(x.timeoutMillis, y.timeoutMillis),
        Monoid[Option[String]].combine(x.dbUrl, y.dbUrl),
        Monoid[Option[String]].combine(x.dbUser, y.dbUser),
        Monoid[Option[String]].combine(x.dbPassword, y.dbPassword),
      )
  }

}
