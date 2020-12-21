package cdecl.declspec

import tofu.logging.Logging
import tofu.logging.zlogs.ZLogs
import zio.{Has, UIO, ULayer, URIO, ZIO, ZLayer}

package object loggerservice {
  type LoggerService = Has[LoggerService.Service]

  object LoggerService {

    case class Service(logger: Logging[UIO])

    val live: ULayer[LoggerService] = ZLayer.fromEffect(ZLogs.uio.forService[LoggerService].map(Service))
  }

  val getLogger: URIO[LoggerService, Logging[UIO]] = ZIO.access(_.get.logger)
}
