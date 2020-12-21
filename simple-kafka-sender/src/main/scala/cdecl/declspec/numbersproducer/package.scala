package cdecl.declspec

import cdecl.declspec.appconfig.AppConfig
import tofu.logging.zlogs.ZLogs
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio.{Has, RIO, Task, URLayer, ZIO, ZLayer, ZRef}

package object numbersproducer {
  type NumbersProducer = Has[NumbersProducer.Service]
  type NumbersProducerDeps = Clock with Random with Blocking
  object NumbersProducer {

    trait Service {
      val run: Task[Unit]
    }

    val live: URLayer[NumbersProducerDeps with AppConfig, NumbersProducer] =
      ZLayer.fromEffect(
        for {
          ref <- ZRef.make[Int](0)
          logger <- ZLogs.uio.byName(NumbersProducer.getClass.toString)
          appSettings <- ZIO.environment[AppConfig]
          deps <- ZIO.environment[Clock with Random with Blocking]
        } yield new LiveNumbersProducer(ref, appSettings.get.config, logger, deps)
      )
  }

  def runNumbersProducer: RIO[NumbersProducer, Unit] = ZIO.accessM(_.get.run)

}
