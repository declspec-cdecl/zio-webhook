package cdecl.declspec.httpservice

import cats.implicits._
import cdecl.declspec.db.subscriptions._
import cdecl.declspec.models.subscriptions.{Subscription, SubscriptionCreate}
import cdecl.declspec.subscriptionqueue._
import zio.{RIO, ZIO}
import sttp.model.Uri

object HttpServiceLogic {


  val listSubscriptions: RIO[Subscriptions, Vector[Subscription]] = getAll

  private def validateScheme(uri: Uri) =
    if (!uri.scheme.toLowerCase.startsWith("http"))
      "unsupported scheme".asLeft[Uri]
    else
      uri.asRight[String]

  private def validate(url: String): Either[NotSupportedError, Uri] =
    Uri.parse(url)
      .flatMap(validateScheme)
      .leftMap(_ => new NotSupportedError)


  def subscribe(subscription: SubscriptionCreate): RIO[SubscriptionQueue with Subscriptions, Unit] =
    for {
      _ <- ZIO.fromEither(validate(subscription.url))
      id <- create(subscription.url)
      _ <- enqueue(Subscribe(Subscription(id, subscription.url)))
    } yield ()

  def unsubscribe(id: Int): RIO[SubscriptionQueue with Subscriptions, Unit] =
    for {
      affected <- delete(id)
      _ <- ZIO.fail(new NotFoundError).when(affected == 0)
      _ <- enqueue(Unsubscribe(Subscription(id, "")))
    } yield ()

  def change(subscription: Subscription): RIO[SubscriptionQueue with Subscriptions, Unit] =
    for {
      _ <- ZIO.fromEither(validate(subscription.url))
      affected <- update(subscription)
      _ <- ZIO.fail(new NotFoundError).when(affected == 0)
      _ <- enqueue(Update(subscription))
    } yield ()

}
