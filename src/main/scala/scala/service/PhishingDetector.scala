package scala.service

import cats.effect._
import io.circe.syntax._
import org.http4s.AuthScheme.Bearer
import org.http4s.Method.POST
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sLiteralsSyntax

import scala.config.AppConfig
import scala.model.PhishingJsonCodecs._
import scala.model._

object PhishingDetector {
  private val cfg = AppConfig.load

  def extractUrls(text: String): List[String] =
    "(https?://[^\\s]+)".r.findAllIn(text).toList

  def isHighConfidence(level: String): Boolean =
    List("HIGH", "HIGHER", "VERY_HIGH", "EXTREMELY_HIGH").contains(level.toUpperCase)

  def isPhishing(url: String, client: Client[IO]): IO[Boolean] = {
    if(cfg.isProdEnv) {
      prodPhishingDetection(url: String, client: Client[IO])
    } else {
      IO.pure(url.contains("phish"))
    }
  }

  private def prodPhishingDetection(url: String, client: Client[IO]): IO[Boolean] = {
    val uri = uri"https://webrisk.googleapis.com/v1eap1:evaluateUri"

    val requestBody = EvaluateUriRequest(
      uri = url,
      threatTypes = List("SOCIAL_ENGINEERING")
    )

    val request = Request[IO](
      method = POST,
      uri = uri,
      headers = Headers(Authorization(Credentials.Token(Bearer, cfg.token)))
    ).withEntity(requestBody.asJson)

    client.expectOr[EvaluateUriResponse](request) { response =>
      response.as[String].flatMap { body =>
        IO.raiseError(new RuntimeException(s"Unexpected response: ${response.status.code}, body: $body"))
      }
    }.map { response =>
      response.scores.exists(score =>
        score.threatType == "SOCIAL_ENGINEERING" && isHighConfidence(score.confidenceLevel)
      )
    }
  }
}
