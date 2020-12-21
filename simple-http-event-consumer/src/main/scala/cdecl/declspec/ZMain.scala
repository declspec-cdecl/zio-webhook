package cdecl.declspec

import zio.{App, ExitCode, URIO }
import cdecl.declspec.httpservice.runHttpServer
import cdecl.declspec.httpservice.HttpService
import tofu.logging.zlogs.ZLogs

object ZMain extends App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    for {
      logger <- ZLogs.uio.byName(ZMain.getClass.toString)
      code <- runHttpServer
        .provideSomeLayer(HttpService.live).as(ExitCode.success)
        .catchAll(e => logger.error(e.getMessage).as(ExitCode.failure))
    } yield code

  }
}
