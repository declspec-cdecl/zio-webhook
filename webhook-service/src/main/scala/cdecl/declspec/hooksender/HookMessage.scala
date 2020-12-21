package cdecl.declspec.hooksender

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class HookMessage(message: String)

object HookMessage{
  implicit val decoder: Decoder[HookMessage] = deriveDecoder
  implicit val encoder: Encoder[HookMessage] = deriveEncoder
}
