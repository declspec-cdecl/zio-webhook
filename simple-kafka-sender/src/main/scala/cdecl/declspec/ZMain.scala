package cdecl.declspec

import zio._
import cdecl.declspec.appconfig.AppConfig
import cdecl.declspec.numbersproducer.{NumbersProducer, runNumbersProducer}
import tofu.logging.zlogs.ZLogs
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random

object ZMain extends App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val layerDeps = AppConfig.live ++ ZLayer.identity[Clock with Blocking with Random]
    val layer = layerDeps >>> NumbersProducer.live

    for {
      logger <- ZLogs.uio.byName(NumbersProducer.getClass.toString)
      code <- runNumbersProducer.provideSomeLayer(layer).as(ExitCode.success)
        .catchAll(e => logger.error(e.getMessage) *> ZIO.succeed(ExitCode.failure))
    } yield code

  }
}
