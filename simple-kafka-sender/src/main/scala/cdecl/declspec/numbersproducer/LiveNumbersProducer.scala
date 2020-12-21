package cdecl.declspec.numbersproducer

import cdecl.declspec.appconfig.AppSettings
import tofu.logging.Logging
import zio.duration._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.random.Random
import zio.{Ref, Schedule, Task, UIO}

import scala.language.postfixOps

class LiveNumbersProducer(counterRef: Ref[Int],
                          appSettings: AppSettings,
                          logger: Logging[UIO],
                          deps: NumbersProducerDeps) extends NumbersProducer.Service {

  private val retrySchedule: Schedule[Random, Any, Any] =
    (Schedule.exponential(3 second).jittered && Schedule.recurs(3)) andThen
      Schedule.spaced(20 seconds)

  val run: Task[Unit] = {
    val producerSettings: ProducerSettings = ProducerSettings(appSettings.kafka.bootStrapServers)

    Producer.make(producerSettings, Serde.string, Serde.string)
      .use(producer =>
        (for {
          counter <- counterRef.get
          _ <- producer.produce(appSettings.kafka.topic, null, counter.toString)
          _ <- counterRef.update(_ => counter + 1)
        } yield ()).repeat(Schedule.spaced(Duration.fromMillis(appSettings.sendingIntervalMillis.toLong))).unit
      )
      .tapError(e => logger.error("kafka producer ended {} ", e.getMessage))
      .retry(retrySchedule)
      .provide(deps)
  }
}
