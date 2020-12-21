package cdecl.declspec.mocks

import tofu.logging.{LoggedValue, Logging}
import zio.{Ref, UIO}

class LoggingMock(stateRef: Ref[Vector[String]]) extends Logging[UIO] {
  override def write(level: Logging.Level, message: String, values: LoggedValue*): UIO[Unit] =
    stateRef.update(state => state :+ message)

  def getLogs: UIO[Vector[String]] = stateRef.get
}
