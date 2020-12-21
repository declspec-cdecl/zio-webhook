package cdecl.declspec.models.subscriptions

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class SubscriptionCreate(url: String)

object SubscriptionCreate{
  implicit val decoder: Decoder[SubscriptionCreate] = deriveDecoder
  implicit val encoder: Encoder[SubscriptionCreate] = deriveEncoder
}
