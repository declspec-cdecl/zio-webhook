package cdecl.declspec

import zio.{Has, Queue, RIO, ULayer, ZIO, ZLayer}

package object subscriptionqueue {

  type SubscriptionQueue = Has[SubscriptionQueue.Service]

  object SubscriptionQueue {

    case class Service(queue: Queue[SubscriptionCommand])

    val live: ULayer[SubscriptionQueue] = ZLayer.fromEffect(
      Queue.unbounded[SubscriptionCommand].map(Service)
    )
  }

  def enqueue(cmd: SubscriptionCommand): RIO[SubscriptionQueue, Boolean] = ZIO.accessM(_.get.queue.offer(cmd))

  def dequeue: RIO[SubscriptionQueue, SubscriptionCommand] = ZIO.accessM(_.get.queue.take)
}
