package cdecl.declspec.httpservice

import cdecl.declspec.models.HookMessage
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.ztapir._
import sttp.tapir.ztapir._
import tofu.logging.Logging
import zio.clock.Clock
import zio.interop.catz._
import zio.{RIO, Task, UIO, ZEnv, ZIO}

class LiveHttpService(deps: zio.ZEnv, logger: Logging[UIO]) extends HttpService.Service {

  val messagesConsumeEndpoint: ZEndpoint[HookMessage, Unit, Unit] = endpoint.post.in(jsonBody[HookMessage])
  val messagesRoutes: HttpRoutes[RIO[Clock, *]] =
    messagesConsumeEndpoint.toRoutes(m => logger.info("{} arrived", m.message))

  val run: Task[Unit] =
    ZIO.runtime[ZEnv].flatMap { implicit runtime =>
      BlazeServerBuilder[RIO[Clock, *]](runtime.platform.executor.asEC)
        .bindHttp(9000,"0.0.0.0")
        .withHttpApp(Router("/" -> messagesRoutes).orNotFound)
        .serve
        .compile
        .drain
    }.provide(deps)

}
