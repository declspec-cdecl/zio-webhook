package cdecl.declspec.mocks

import cdecl.declspec.hooksender.HookSender
import zio.{Ref, Task, UIO, ZIO}

class HookSenderMock(stateRef: Ref[(Vector[Boolean], Vector[String])]) extends HookSender.Service {
  override def sendMessage(message: String): Task[Unit] =
    for {
      isSuccess <- stateRef.modify {
        case (x +: xs, ys) => (x, (xs, ys :+ message))
        case (_, ys) => (true, (Vector.empty, ys :+ message))
      }
      _ <- ZIO.fail(new Throwable("description")).when(!isSuccess)
    } yield ()

  def getSent: UIO[Vector[String]] = stateRef.get.map(_._2)
}
