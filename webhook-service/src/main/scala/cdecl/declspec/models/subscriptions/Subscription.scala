package cdecl.declspec.models.subscriptions

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Subscription(id: Int, url: String)

object Subscription {
  implicit val decoder: Decoder[Subscription] = deriveDecoder
  implicit val encoder: Encoder[Subscription] = deriveEncoder
}
