package scala.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object PhishingJsonCodecs {
  implicit val evaluateUriRequestEncoder: Encoder[EvaluateUriRequest] = deriveEncoder
  implicit val scoreDecoder: Decoder[Score] = deriveDecoder
  implicit val evaluateUriResponseDecoder: Decoder[EvaluateUriResponse] = deriveDecoder
}
