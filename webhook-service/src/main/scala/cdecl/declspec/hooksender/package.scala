package cdecl.declspec

import zio.{Has, RIO, Task, ZIO}

package object hooksender {

  type HookSender = Has[HookSender.Service]

  object HookSender {

    trait Service {
      def sendMessage(message: String): Task[Unit]
    }
  }

  def sendMessage(message: String): RIO[HookSender, Unit] = ZIO.accessM[HookSender](_.get.sendMessage(message))
}
