package cdecl.declspec.workerfactory

import cdecl.declspec.hooksender.{HookSender, sendMessage}
import tofu.logging.Logging
import zio.blocking.Blocking
import zio.clock.Clock
import zio.kafka.consumer.{Consumer, Subscription}
import zio.kafka.serde.Serde
import zio.random.Random
import zio.{UIO, ZIO}

object KafkaWorker {
  type KafkaWorkerDeps = Blocking with Clock with Random with Consumer with HookSender


  def getKafkaEventsHandler(settings: HookWorkerSettings,
                            logger: Logging[UIO]): ZIO[KafkaWorkerDeps, Throwable, Unit] =
    Consumer.subscribeAnd(Subscription.topics(settings.kafka.common.topic))
      .plainStream(Serde.string, Serde.string)
      .tap(cr => logger.debug("got message {} for {}", cr.value, settings.hook.url)
        *> handleMessage(cr.value, settings.hook.retrySeconds, logger))
      .map(_.offset)
      .aggregateAsync(Consumer.offsetBatches)
      .mapM(_.commit)
      .runDrain

  def handleMessage(msg: String,
                    retrySeconds: Int,
                    logger: Logging[UIO]): ZIO[Random with Clock with HookSender, Throwable, Unit] = {
    sendMessage(msg).tapError(e => logger.warn(e.getMessage)).retry(getRetrySchedule(retrySeconds))
  }

}
