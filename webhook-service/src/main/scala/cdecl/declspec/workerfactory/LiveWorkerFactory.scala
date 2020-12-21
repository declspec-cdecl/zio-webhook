package cdecl.declspec.workerfactory


import cdecl.declspec.hooksender.{HookSender, LiveHookSender}
import cdecl.declspec.workerfactory.KafkaWorker.getKafkaEventsHandler
import tofu.logging.Logging
import tofu.logging.zlogs.ZLogs
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.{IO, UIO, URIO, ZLayer}

class LiveWorkerFactory(deps: WorkerFactoryDeps) extends WorkerFactory.Service {

  private def getConsumerSettings(kafka: KafkaSettings): ConsumerSettings =
    ConsumerSettings(kafka.common.bootStrapServers)
      .withGroupId(kafka.groupName)
      .withClientId(s"client${kafka.groupName}")

  private val getLogger = ZLogs.uio.byName(KafkaWorker.getClass.toString)

  private def runWorker(service: Consumer.Service,
                        hookSender: HookSender.Service,
                        settings: HookWorkerSettings,
                        logger: Logging[UIO]) = {
    val depLayer = ZLayer.fromEffect(URIO(service)) ++ ZLayer.fromEffect(URIO(hookSender))
    getKafkaEventsHandler(settings, logger)
      .provideSomeLayer[WorkerFactoryDeps](depLayer)
      .provide(deps)
  }


  def startWorker(settings: HookWorkerSettings): IO[Throwable, WorkerEntry] = {

    val consumerSettings = getConsumerSettings(settings.kafka)
    val hookSender: HookSender.Service = new LiveHookSender(settings.hook, deps)

    getLogger.flatMap(logger => {
      Consumer.make(consumerSettings)
        .use(runWorker(_, hookSender, settings, logger))
        .tapError(e => logger.error("kafka consumer ended {} ", e.getMessage))
        .retry(getRetrySchedule(settings.hook.retrySeconds)).fork
        .map(fiber => WorkerEntry(settings.hook.url, fiber))
        .provide(deps)
    })

  }
}
