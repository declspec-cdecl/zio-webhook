package cdecl.declspec

import tofu.logging.zlogs.ZLogs
import zio.{Has, RIO, RLayer, Task, ZIO, ZLayer}

package object httpservice {

  type HttpService = Has[HttpService.Service]

  object HttpService {

    trait Service {
      val run: Task[ Unit]
    }

    val live: RLayer[zio.ZEnv, HttpService] = ZLayer.fromEffect(
      for {
        deps <-ZIO.environment[zio.ZEnv]
        logger <- ZLogs.uio.byName(HttpService.getClass.toString)
      } yield new LiveHttpService(deps,logger))
  }
  val runHttpServer:RIO[HttpService ,Unit] = ZIO.accessM(_.get.run)

}
