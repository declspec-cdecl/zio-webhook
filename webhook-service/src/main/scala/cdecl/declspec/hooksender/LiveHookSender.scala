package cdecl.declspec.hooksender

import cdecl.declspec.workerfactory.WebhookSettings
import io.circe.syntax._
import sttp.client._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.Uri
import zio.clock.Clock
import zio.duration._
import zio.{Task, ZIO}

class LiveHookSender(settings: WebhookSettings, clock: Clock) extends HookSender.Service {

  private def getResponse(request: RequestT[Identity, Either[String, String], Nothing]) =
    AsyncHttpClientZioBackend()
      .flatMap { implicit backend => request.send().timeout(Duration.fromMillis(settings.timeoutMillis)) }

  override def sendMessage(message: String): Task[Unit] = {
    val program =
      for {
        uri <- ZIO.fromEither(Uri.parse(settings.url)).mapError(e => new Exception(e))
        request = basicRequest.post(uri).body(HookMessage(message).asJson.noSpaces)
        responseOpt <- getResponse(request)
        response <- ZIO.fromOption(responseOpt).mapError(_ => new Exception(s"${settings.url} timeout"))
        _ <- ZIO.fail(new Exception(s"${settings.url} returned not 200")).when(!response.is200)
      } yield ()
    program.provide(clock)
  }
}
