package cdecl.declspec.httplogictests

import cdecl.declspec.models.subscriptions.Subscription
import cdecl.declspec.subscriptionqueue.SubscriptionCommand

case class HttpLogicSideEffectState[A](command: Option[SubscriptionCommand], subscriptions: Vector[Subscription], result: A)
