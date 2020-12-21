package cdecl.declspec.subscriptionqueue

import cdecl.declspec.models.subscriptions.Subscription

sealed trait SubscriptionCommand extends Product with Serializable

final case class Subscribe(subscription: Subscription) extends SubscriptionCommand

final case class Unsubscribe(subscription: Subscription) extends SubscriptionCommand

final case class Update(subscription: Subscription) extends SubscriptionCommand
