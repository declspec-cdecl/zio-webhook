package cdecl.declspec.models

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

case class HookMessage(message: String)

object HookMessage{
  implicit val decoder: Decoder[HookMessage] = deriveDecoder
}
