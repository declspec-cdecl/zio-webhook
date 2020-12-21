package cdecl.declspec.mocks

import cdecl.declspec.models.subscriptions.Subscription

case class SubscriptionsState(data: Map[Int, Subscription] = Map(), curId: Int = 1)
