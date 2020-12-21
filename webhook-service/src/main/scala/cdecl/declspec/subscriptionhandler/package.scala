package cdecl.declspec

import cdecl.declspec.appconfig.AppConfig
import cdecl.declspec.db.subscriptions.Subscriptions
import cdecl.declspec.models.subscriptions.Subscription
import cdecl.declspec.subscriptionqueue.{Subscribe, SubscriptionCommand, SubscriptionQueue}
import cdecl.declspec.workerfactory.{WorkerEntry, WorkerFactory}
import tofu.logging.zlogs.ZLogs
import zio._

package object subscriptionhandler {

  type SubscriptionHandler = Has[SubscriptionHandler.Service]
  type SubscriptionHandlerDeps = SubscriptionQueue with WorkerFactory with AppConfig
  type SubscriptionHandlerCreationDeps = SubscriptionHandlerDeps with Subscriptions

  object SubscriptionHandler {

    trait Service {
      def run: UIO[Nothing]
    }

    private def subscribe(subscriptions: Vector[Subscription], queue: Queue[SubscriptionCommand]): Task[Unit] = {
      subscriptions match {
        case x +: xs => queue.offer(Subscribe(x)) *> subscribe(xs, queue)
        case _ => ZIO.unit
      }
    }

    val live: RLayer[SubscriptionHandlerCreationDeps, SubscriptionHandler] = ZLayer.fromEffect(
      for {
        deps <- ZIO.environment[SubscriptionHandlerCreationDeps]
        logger <- ZLogs.uio.byName(SubscriptionHandler.getClass.toString)
        workersRef <- ZRef.make(Map[Int, WorkerEntry]())
        subs <- deps.get[Subscriptions.Service].getAll
        _ <- subscribe(subs, deps.get[SubscriptionQueue.Service].queue)
      } yield new LiveSubscriptionHandler(deps, logger, workersRef)
    )
  }

  def runSubsHandler: RIO[SubscriptionHandler, Nothing] = ZIO.accessM(_.get.run)
}
