package scala.model

import io.circe.generic.JsonCodec

@JsonCodec case class Sms(sender: String, recipient: String, message: String)
