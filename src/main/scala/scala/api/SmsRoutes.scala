package scala.api

import cats.effect._
import cats.implicits._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.{HttpRoutes, _}

import scala.config.AppConfig
import scala.model.{Sms, SmsCategory}
import scala.service.{PhishingDetector, SmsStorage, SubscriptionService}

object SmsRoutes {

  implicit val smsDecoder = jsonOf[IO, Sms]
  private val cfg = AppConfig.load

  def routes(client: Client[IO], subs: SubscriptionService, storage: SmsStorage): HttpRoutes[IO] = HttpRoutes.of[IO] {

    case req@POST -> Root / "sms" =>
      for {
        sms <- req.as[Sms]
        response <- {
          if (sms.recipient == cfg.subscriptionPhone) {
            handleSubscriptionMessage(sms, subs)
          } else {
            handlePhishingDetection(sms, client, subs, storage)
          }
        }
      } yield response
  }

  private def handleSubscriptionMessage(sms: Sms, subs: SubscriptionService): IO[Response[IO]] = {
    sms.message.trim.toUpperCase match {
      case "START" =>
        subs.subscribe(sms.sender) *> Ok("Subscription activated.")
      case "STOP" =>
        subs.unsubscribe(sms.sender) *> Ok("Subscription deactivated.")
      case _ => Ok("Unrecognized command.")
    }
  }

  private def handlePhishingDetection(sms: Sms, client: Client[IO], subs: SubscriptionService, storage: SmsStorage): IO[Response[IO]] = {
    val urls = PhishingDetector.extractUrls(sms.message)

    for {
      subscribed <- subs.isSubscribed(sms.recipient)
      phishingResults <- urls.traverse(url => PhishingDetector.isPhishing(url, client))
      response <- {
        if (!subscribed) {
          storage.save(sms, urls, SmsCategory.Unsubscribed) *>
          Ok("User not subscribed.")
        } else if (phishingResults.contains(true)) {
          storage.save(sms, urls, SmsCategory.Phishing) *>
          Ok("Phishing message detected.")
        } else {
          storage.save(sms, urls, SmsCategory.Accepted) *>
          Ok("Message accepted.")
        }
      }
    } yield response
  }
}
