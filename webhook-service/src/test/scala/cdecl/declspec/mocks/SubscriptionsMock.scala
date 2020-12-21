package cdecl.declspec.mocks

import cdecl.declspec.db.subscriptions.Subscriptions
import cdecl.declspec.models.subscriptions.Subscription
import zio.{Ref, Task}

class SubscriptionsMock(storeRef: Ref[SubscriptionsState]) extends Subscriptions.Service {
  override def create(url: String): Task[Int] =
    storeRef.modify(state =>
      (state.curId, SubscriptionsState(state.data + (state.curId -> Subscription(state.curId, url)), state.curId + 1)))

  override def getAll: Task[Vector[Subscription]] =
    storeRef.get.map(s => s.data.values.toVector)

  override def delete(id: Int): Task[Int] =
    storeRef.modify(state => {
      state.data.get(id) match {
        case Some(_) => (1, state.copy(data = state.data.removed(id)))
        case None => (0, state)
      }
    })

  override def update(subscription: Subscription): Task[Int] =
    storeRef.modify(state =>
      state.data.get(subscription.id) match {
        case Some(_) => (1, state.copy(data = state.data.updated(subscription.id, subscription)))
        case None => (0, state)
      })
}
