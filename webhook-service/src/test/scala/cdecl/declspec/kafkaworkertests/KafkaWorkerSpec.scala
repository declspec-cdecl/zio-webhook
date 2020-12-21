package cdecl.declspec.kafkaworkertests

import cdecl.declspec.mocks.{HookSenderMock, LoggingMock}
import cdecl.declspec.hooksender.HookSender
import cdecl.declspec.workerfactory.KafkaWorker.handleMessage
import tofu.logging.Logging
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.Assertion._
import zio.test._
import zio._
import zio.clock.Clock
import zio.random.Random
import scala.language.postfixOps
import zio.duration._

object KafkaWorkerSpec extends DefaultRunnableSpec {
  def runKafkaWorkerOp(program: Logging[UIO] => ZIO[Random with Clock with HookSender, Throwable, Unit], failureMap: Vector[Boolean] = Vector()):
  ZIO[Random with Clock with TestClock, Throwable, KafkaWorkerEffectState] = {
    for {
      loggingRef <- ZRef.make(Vector[String]())
      logging = new LoggingMock(loggingRef)
      senderRef <- ZRef.make((failureMap, Vector[String]()))
      sender = new HookSenderMock(senderRef)
      layer = ZLayer.fromEffect(ZIO.succeed(sender: HookSender.Service)) ++ ZLayer.identity[Random with Clock]
      fiber <- program(logging).provideSomeLayer(layer).fork
      _ <- TestClock.adjust(30 second)
      _ <- fiber.join
      logs <- logging.getLogs
      sent <- sender.getSent
    } yield KafkaWorkerEffectState(sent, logs)
  }

  override def spec: ZSpec[TestEnvironment, Any] = suite("KafkaWorkerSpec")(
    testM("successful send message") {
      val message = "msg"
      runKafkaWorkerOp(logger => handleMessage(message, 1, logger)).map {
        case KafkaWorkerEffectState(sent, logs) =>
          assert(logs)(isEmpty) && assert(sent)(contains(message))
      }
    },
    testM("logging failure") {
      val message = "msg"

      runKafkaWorkerOp(logger => handleMessage(message, 1, logger), Vector(false)).map {
        case KafkaWorkerEffectState(sent, logs) =>
          assert(logs)(not(isEmpty)) && assert(sent)(contains(message))
      }
    }

  )
}
