package cdecl.declspec

import java.util.concurrent.TimeUnit

import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.{Duration, _}
import zio.random.Random

import scala.language.postfixOps


package object workerfactory {

  def getRetrySchedule(retryIntervalSeconds: Int): Schedule[Random, Any, Any] =
    (Schedule.exponential(3 second).jittered && Schedule.recurs(3)) andThen
      Schedule.spaced(Duration(retryIntervalSeconds, TimeUnit.SECONDS))

  type WorkerFactory = Has[WorkerFactory.Service]
  type WorkerFactoryDeps = Blocking with Clock with Random

  object WorkerFactory {

    trait Service {
      def startWorker(settings: HookWorkerSettings): IO[Throwable, WorkerEntry]
    }

    val live: URLayer[Blocking with Clock with Random, WorkerFactory] =
      ZLayer.fromEffect(
        ZIO.environment[WorkerFactoryDeps]
          .map(d => new LiveWorkerFactory(d))
      )
  }

  def startWorker(settings: HookWorkerSettings): RIO[WorkerFactory, WorkerEntry] =
    ZIO.accessM[WorkerFactory](_.get.startWorker(settings))
}
