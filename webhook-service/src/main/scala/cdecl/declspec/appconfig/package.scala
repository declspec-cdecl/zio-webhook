package cdecl.declspec

import tofu.logging.zlogs.ZLogs
import zio._

package object appconfig {
  type AppConfig = Has[AppConfig.Service]

  object AppConfig {

    case class Service(config: AppSettings)

    val live: RLayer[system.System, AppConfig] = ZLayer.fromEffect(
      for {
        envs <- system.envs
        appSettings <- ZIO.fromEither(AppSettings.fromEnvs(envs)).mapError(e => new Exception(e))
        logger <- ZLogs.uio.byName(AppConfig.getClass.toString)
        _ <- logger.info("appsettings {}",appSettings)
      } yield Service(appSettings)
    )
  }

  val appSettings: RIO[AppConfig, AppSettings] = ZIO.access(_.get.config)

}
