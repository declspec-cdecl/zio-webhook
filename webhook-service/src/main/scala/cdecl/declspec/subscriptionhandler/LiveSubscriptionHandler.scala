package cdecl.declspec.subscriptionhandler

import cdecl.declspec.appconfig.{AppConfig, AppSettings}
import cdecl.declspec.models.subscriptions.Subscription
import cdecl.declspec.subscriptionhandler.WorkerSettingsFactory.getWorkerSettings
import cdecl.declspec.subscriptionqueue._
import cdecl.declspec.workerfactory.{HookWorkerSettings, WorkerEntry, WorkerFactory, startWorker}
import tofu.logging.Logging
import cats.implicits._
import zio.{Queue, Ref, UIO, ZIO}

class LiveSubscriptionHandler(deps: SubscriptionHandlerDeps,
                              logger: Logging[UIO],
                              workersRef: Ref[Map[Int, WorkerEntry]]) extends SubscriptionHandler.Service {


  private def getAddingError(subscription: Subscription) =
    new Exception(s"Entry already exists for id ${subscription.id} and url ${subscription.url} ")

  private def getRemovingError(subscription: Subscription) =
    new Exception(s"Entry doesn't exist for ${subscription.id}")

  private def addEntry(workers: Map[Int, WorkerEntry], entry: WorkerEntry, subscription: Subscription) =
    workers.get(subscription.id) match {
      case Some(_) => (Some(getAddingError(subscription)), workers)
      case None => (None, workers + (subscription.id -> entry))
    }

  private def removeEntry(workers: Map[Int, WorkerEntry], subscription: Subscription) =
    workers.get(subscription.id) match {
      case Some(entry) => (entry.asRight[Exception], workers.removed(subscription.id))
      case None => (getRemovingError(subscription).asLeft[WorkerEntry], workers)
    }

  private def handleStart(workerSettings: HookWorkerSettings, subscription: Subscription): ZIO[WorkerFactory, Throwable, Unit] = {
    for {
      entry <- startWorker(workerSettings)
      errorOpt <- workersRef.modify(addEntry(_, entry, subscription))
      _ <- errorOpt match {
        case Some(error) => entry.fiber.interrupt *> ZIO.fail(error)
        case None => logger.info("worker has started for subscription {} {}",subscription.id,subscription.url)
      }
    } yield ()
  }

  private def handleStop(subscription: Subscription): ZIO[Any, Exception, Unit] = {
    for {
      removeResult <- workersRef.modify(removeEntry(_, subscription))
      entry <- ZIO.fromEither(removeResult)

      _ <- entry.fiber.interrupt *> logger.info("worker has stopped for subscription {}",subscription.id)
    } yield ()
  }

  private def handleCommand(command: SubscriptionCommand, settings: AppSettings): ZIO[WorkerFactory, Throwable, Unit] = {
    command match {
      case Subscribe(subscription) => handleStart(getWorkerSettings(settings, subscription), subscription)
      case Unsubscribe(subscription) => handleStop(subscription)
      case Update(subscription) => handleStop(subscription) *>
        handleStart(getWorkerSettings(settings, subscription), subscription)
    }
  }

  private def handleCommandFromQueue(queue: Queue[SubscriptionCommand], settings: AppSettings) = {
    val program =
      for {
        cmd <- queue.take
        _ <- logger.info(s"Subscription command: {}", cmd.toString)
        _ <- handleCommand(cmd, settings).catchAll(e => logger.error(e.getMessage))
      } yield ()
    program.forever
  }

  def run: UIO[Nothing] = {
    val program =
      ZIO.environment[SubscriptionQueue]
        .map(_.get.queue)
        .zip(ZIO.environment[AppConfig].map(_.get.config))
        .flatMap { case (queue, settings) =>
          handleCommandFromQueue(queue, settings)
        }
    program.provide(deps)

  }
}
