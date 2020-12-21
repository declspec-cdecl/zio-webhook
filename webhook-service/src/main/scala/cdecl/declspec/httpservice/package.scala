package cdecl.declspec

import cdecl.declspec.appconfig.AppConfig
import cdecl.declspec.db.subscriptions.Subscriptions
import cdecl.declspec.httpservice.HttpService
import cdecl.declspec.subscriptionqueue.SubscriptionQueue
import tofu.logging.zlogs.ZLogs
import zio._

package object httpservice {
  type HttpEnv = Subscriptions with SubscriptionQueue

  type HttpService = Has[HttpService.Service]
  type HttpServiceDeps = Subscriptions with SubscriptionQueue with AppConfig

  object HttpService {

    trait Service {
      val run: Task[Unit]
    }

    val live: URLayer[HttpServiceDeps with zio.ZEnv, HttpService] = ZLayer.fromEffect(
      for {
        deps <- ZIO.environment[zio.ZEnv with HttpEnv]
        logger <- ZLogs.uio.byName(HttpService.getClass.toString)
      } yield new LiveHttpService(deps, logger)
    )
  }

  val runHttpServer: RIO[HttpService, Unit] = ZIO.accessM(_.get.run)

}
