package cdecl.declspec.db.subscriptions

import cats.effect.Concurrent
import cdecl.declspec.models.subscriptions.Subscription
import doobie.implicits._
import doobie.util.transactor.Transactor
import zio.Task
import zio.interop.catz

class LiveSubscriptionPersistence(xa: Transactor[Task]) extends Subscriptions.Service {

  import LiveSubscriptionPersistence._

  override def create(url: String): Task[Int] =
    SQL.insert(url).withUniqueGeneratedKeys[Int](""""id"""").transact(xa)

  override def getAll: Task[Vector[Subscription]] =
    SQL.selectAll().to[Vector].transact(xa)


  override def delete(id: Int): Task[Int] =
    SQL.delete(id).run.transact(xa)


  override def update(subscription: Subscription): Task[Int] =
    SQL.update(subscription).run.transact(xa)
}

object LiveSubscriptionPersistence {
  final implicit val taskMonad: Concurrent[Task] = catz.taskConcurrentInstance

  object SQL {

    def insert(url: String): doobie.Update0 =
      sql"""INSERT INTO "Subscriptions" (url) VALUES ($url) RETURNING "id";""".update

    def selectAll(): doobie.Query0[Subscription] =
      sql"""SELECT "id","url" FROM "Subscriptions";""".query[Subscription]

    def delete(id: Int): doobie.Update0 =
      sql"""DELETE FROM "Subscriptions" WHERE "id"=$id;""".update

    def update(subscription:Subscription): doobie.Update0 =
      sql"""UPDATE "Subscriptions" SET "url"=${subscription.url} WHERE "id"=${subscription.id}; """.update

  }

}
