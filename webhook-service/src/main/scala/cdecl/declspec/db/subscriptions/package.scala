package cdecl.declspec.db

import cdecl.declspec.models.subscriptions.Subscription
import zio.{Has, RIO, Task, URLayer, ZIO, ZLayer}

package object subscriptions {
  type Subscriptions = Has[Subscriptions.Service]

  object Subscriptions {

    trait Service {
      def create(url: String): Task[Int]

      def getAll: Task[Vector[Subscription]]

      def delete(id: Int): Task[Int]

      def update(subscription: Subscription): Task[Int]
    }

    val live: URLayer[DB, Subscriptions] = ZLayer.fromEffect(ZIO.access(r => new LiveSubscriptionPersistence(r.get.transactor)))
  }

  def getAll: RIO[Subscriptions, Vector[Subscription]] = ZIO.accessM(_.get.getAll)

  def create(url: String): RIO[Subscriptions, Int] = ZIO.accessM(_.get.create(url))

  def delete(id: Int): RIO[Subscriptions, Int] = ZIO.accessM(_.get.delete(id))

  def update(subscription: Subscription): RIO[Subscriptions, Int] = ZIO.accessM(_.get.update(subscription))
}
