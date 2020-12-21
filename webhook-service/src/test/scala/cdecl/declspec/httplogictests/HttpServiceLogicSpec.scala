package cdecl.declspec.httplogictests

import cdecl.declspec.db.subscriptions.Subscriptions
import cdecl.declspec.httpservice.NotSupportedError
import cdecl.declspec.httpservice.NotFoundError
import cdecl.declspec.httpservice.HttpServiceLogic._
import cdecl.declspec.mocks.{SubscriptionsMock, SubscriptionsState}
import cdecl.declspec.models.subscriptions.{Subscription, SubscriptionCreate}
import cdecl.declspec.subscriptionqueue
import cdecl.declspec.subscriptionqueue.{Subscribe, SubscriptionCommand, SubscriptionQueue, Unsubscribe, Update}
import zio.clock.Clock
import zio.duration._
import zio.test.Assertion._
import zio.test._
import zio._

import scala.language.postfixOps


case object HttpServiceLogicSpec extends DefaultRunnableSpec {

  private def runHttpLogicOp[A](program: RIO[SubscriptionQueue with Subscriptions, A],
                                startState: SubscriptionsState = SubscriptionsState()): ZIO[Clock, Throwable, HttpLogicSideEffectState[A]] = for {
    mockState <- ZRef.make(startState)
    subsMockSvc = new SubscriptionsMock(mockState)
    queue <- Queue.unbounded[SubscriptionCommand]
    queueSvc = subscriptionqueue.SubscriptionQueue.Service(queue)
    layer: ULayer[SubscriptionQueue with Subscriptions] =
    ZLayer.fromEffect(ZIO.succeed(subsMockSvc: Subscriptions.Service)) ++ ZLayer.fromEffect(ZIO.succeed(queueSvc))
    res <- program.provideSomeLayer(layer)

    commandOpt <- queue.take.timeout(Duration.fromMillis(1))
    storedSubscriptions <- subsMockSvc.getAll
  } yield HttpLogicSideEffectState[A](commandOpt, storedSubscriptions, res)

  override def spec: ZSpec[zio.test.environment.TestEnvironment, Any] = suite("HttpServiceLogicSpec")(
    testM("successful subscribe") {
      val subscription = SubscriptionCreate("http://localhost:9090")
      val storedSubscription = Subscription(1, subscription.url)
      val createdCommand = Subscribe(storedSubscription)

      runHttpLogicOp(subscribe(subscription)).map {
        case HttpLogicSideEffectState(commandOpt, storedSubscriptions, _) =>
          assert(commandOpt)(isSome(Assertion.equalTo(createdCommand))) &&
            assert(storedSubscriptions)(contains(storedSubscription))
      }
    },
    testM("wrong url subscribe") {

      val subscription = SubscriptionCreate("wrong url")
      assertM(runHttpLogicOp(subscribe(subscription)).run)(fails(isSubtype[NotSupportedError](Assertion.anything)))

    },
    testM("successful unsubscribe") {
      val toUnsubscribe = Subscription(1, "http://localhost:9090")
      val storedSubscriptions = Vector(toUnsubscribe, Subscription(2, "http://localhost:9091"))

      val subsState = SubscriptionsState(storedSubscriptions.map(s => (s.id, s)).toMap, 3)

      runHttpLogicOp(unsubscribe(toUnsubscribe.id), subsState).map {
        case HttpLogicSideEffectState(commandOpt, storedSubscriptions, _) =>
          assert(commandOpt)(isSome(isSubtype[Unsubscribe](hasField("id", _.subscription.id, equalTo(toUnsubscribe.id))))) &&
            assert(storedSubscriptions)(not(contains(toUnsubscribe)))
      }
    },
    testM("unsubscribe not existing") {
      val toUnsubscribe = Subscription(1, "http://localhost:9090")
      val storedSubscriptions = Vector(toUnsubscribe, Subscription(2, "http://localhost:9091"))

      val subsState = SubscriptionsState(storedSubscriptions.map(s => (s.id, s)).toMap, 3)

      assertM(runHttpLogicOp(unsubscribe(3), subsState).run)(fails(isSubtype[NotFoundError](Assertion.anything)))
    },
    testM("successful change") {
      val toChange = Subscription(1, "http://localhost:9090")
      val storedSubscriptions = Vector(toChange, Subscription(2, "http://localhost:9091"))
      val changedSubscription = toChange.copy(url = "http://localhost:9090")
      val createdCommand = Update(changedSubscription)

      val subsState = SubscriptionsState(storedSubscriptions.map(s => (s.id, s)).toMap, 3)

      runHttpLogicOp(change(changedSubscription), subsState).map {
        case HttpLogicSideEffectState(commandOpt, storedSubscriptions, _) =>
          assert(commandOpt)(isSome(isSubtype[Update](equalTo(createdCommand)))) &&
            assert(storedSubscriptions)(contains(changedSubscription))
      }
    },
    testM("change not existing") {
      val storedSubscriptions = Vector(Subscription(1, "http://localhost:9090"), Subscription(2, "http://localhost:9091"))
      val changedSubscription = Subscription(5, "http://localhost:9090")

      val subsState = SubscriptionsState(storedSubscriptions.map(s => (s.id, s)).toMap, 3)
      assertM(runHttpLogicOp(change(changedSubscription), subsState).run)(fails(isSubtype[NotFoundError](Assertion.anything)))
    },
    testM("change with bad url") {
      val toChange = Subscription(1, "http://localhost:9090")
      val storedSubscriptions = Vector(toChange, Subscription(2, "http://localhost:9091"))
      val changedSubscription = toChange.copy(url = "bad url")

      val subsState = SubscriptionsState(storedSubscriptions.map(s => (s.id, s)).toMap, 3)

      assertM(runHttpLogicOp(change(changedSubscription), subsState).run)(fails(isSubtype[NotSupportedError](Assertion.anything)))
    }


  )
}
