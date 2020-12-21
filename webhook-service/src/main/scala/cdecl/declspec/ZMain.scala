package cdecl.declspec

import cdecl.declspec.appconfig.AppConfig
import cdecl.declspec.db.DB
import cdecl.declspec.db.subscriptions.Subscriptions
import cdecl.declspec.httpservice.{HttpService, runHttpServer}
import cdecl.declspec.subscriptionhandler.{SubscriptionHandler, runSubsHandler}
import cdecl.declspec.subscriptionqueue.SubscriptionQueue
import cdecl.declspec.workerfactory.WorkerFactory
import tofu.logging.zlogs.ZLogs
import zio.blocking.Blocking
import zio.{ZEnv, _}

object ZMain extends App {


  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {

    val dbLayer = AppConfig.live ++ ZLayer.identity[Blocking] >>> DB.live
    val subscriptionStoreLayer = dbLayer >>> Subscriptions.live
    val subscriptionLayerDeps = SubscriptionQueue.live ++ WorkerFactory.live ++ AppConfig.live ++ subscriptionStoreLayer
    val subscriptionLayer = subscriptionLayerDeps >>> SubscriptionHandler.live

    val httpLayerDeps = SubscriptionQueue.live ++ subscriptionStoreLayer ++ ZLayer.identity[ZEnv] ++ AppConfig.live
    val httpLayer = httpLayerDeps >>> HttpService.live

    val appLayer = subscriptionLayer ++ httpLayer

    for {
      logger <- ZLogs.uio.byName(ZMain.getClass.toString)
      code <- runSubsHandler.zipPar(runHttpServer)
        .provideSomeLayer(appLayer).as(ExitCode.success)
        .catchAll(e => logger.error(e.getMessage) *> ZIO.succeed(ExitCode.failure))
    } yield code
  }
}
