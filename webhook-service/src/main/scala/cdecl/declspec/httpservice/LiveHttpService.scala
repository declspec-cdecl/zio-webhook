package cdecl.declspec.httpservice

import cats.syntax.all._
import cdecl.declspec.db.subscriptions.Subscriptions
import cdecl.declspec.models.subscriptions.{Subscription, SubscriptionCreate}
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.kleisli._
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.ztapir._
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import sttp.tapir.ztapir._
import tofu.logging.Logging
import zio._
import zio.clock.Clock
import zio.interop.catz._


class LiveHttpService(deps: zio.ZEnv with HttpEnv, logger: Logging[UIO]) extends HttpService.Service {

  import HttpServiceLogic._

  val baseEndpoint = endpoint.in("api" / "1.0")

  val listEndpoint =
    baseEndpoint
      .get
      .out(jsonBody[Vector[Subscription]] and statusCode
        .description(StatusCode.Ok, "hook list"))
      .name("hook list").description("gets hook list")

  val listServerEndpoint: ZServerEndpoint[Subscriptions, Unit, Unit, (Vector[Subscription], StatusCode)] =
    listEndpoint.zServerLogic(_ =>
      listSubscriptions
        .map(s => (s, StatusCode.Ok))
        .mapError(e => logger.error(e.getMessage).unit))

  val createEndpoint =
    baseEndpoint
      .post
      .in(jsonBody[SubscriptionCreate])
      .out(statusCode
        .description(StatusCode.Ok, "hook has been created")
        .description(StatusCode.BadRequest, "wrong data or unsupported schema"))
      .name("crate hook")
      .description("creates hook")

  val createServerEndpoint: ZServerEndpoint[HttpEnv, SubscriptionCreate, Unit, StatusCode] =
    createEndpoint.zServerLogic(s =>
      subscribe(s).as(StatusCode.Ok)
        .catchSome { case _: NotSupportedError => ZIO.succeed(StatusCode.BadRequest) }
        .mapError(e => logger.error(e.getMessage).unit))

  val updateEndpoint =
    baseEndpoint
      .patch
      .in(jsonBody[Subscription])
      .out(statusCode
        .description(StatusCode.Ok, "hook has been updated")
        .description(StatusCode.NotFound, "there is no such hook")
        .description(StatusCode.BadRequest, "wrong data or unsupported schema"))
      .name("update hook")
      .description("updates hook")
  val updateServerEndpoint: ZServerEndpoint[HttpEnv, Subscription, Unit, StatusCode] =
    updateEndpoint.zServerLogic(s =>
      change(s).as(StatusCode.Ok)
        .catchSome { case _: NotFoundError => ZIO.succeed(StatusCode.NotFound) }
        .mapError(e => logger.error(e.getMessage).unit))

  val deleteEndpoint =
    baseEndpoint
      .delete
      .in(path[Int].name("id"))
      .out(statusCode
        .description(StatusCode.Ok, "hook has been deleted")
        .description(StatusCode.NotFound, "there is no such hook"))
      .name("delete hook")
      .description("deletes hook")
  val deleteServerEndpoint: ZServerEndpoint[HttpEnv, Int, Unit, StatusCode] =
    deleteEndpoint.zServerLogic(id =>
      unsubscribe(id).as(StatusCode.Ok)
        .catchSome { case _: NotFoundError => ZIO.succeed(StatusCode.NotFound) }
        .mapError(e => logger.error(e.getMessage).unit))


  val routesList = List(
    listServerEndpoint.widen[HttpEnv],
    createServerEndpoint,
    updateServerEndpoint,
    deleteServerEndpoint
  )
  val routes: HttpRoutes[RIO[HttpEnv with Clock, *]] =
    routesList.toRoutes

  val yaml: String = {
    import sttp.tapir.docs.openapi._
    import sttp.tapir.openapi.circe.yaml._
    routesList.toOpenAPI("Webhook demo", "0.1").toYaml
  }

  val run: Task[Unit] =
    ZIO.runtime[ZEnv with HttpEnv].flatMap { implicit runtime =>
      BlazeServerBuilder[RIO[HttpEnv with Clock, *]](runtime.platform.executor.asEC)
        .bindHttp(9000, "0.0.0.0")
        .withHttpApp(Router("/" -> (routes <+> new SwaggerHttp4s(yaml).routes)).orNotFound)
        .serve
        .compile
        .drain
    }.provide(deps)
}
