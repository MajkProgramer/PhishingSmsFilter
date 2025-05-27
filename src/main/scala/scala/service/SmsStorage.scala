package scala.service

import cats.effect.IO

import scala.db.Database
import scala.model.{Sms, SmsCategory}

trait SmsStorage {
  def save(sms: Sms, urls: List[String], category: SmsCategory): IO[Unit]
}

class PostgresSmsStorage extends SmsStorage {

  override def save(sms: Sms, urls: List[String], category: SmsCategory): IO[Unit] = IO {
    val detected = category == SmsCategory.Phishing
    val status = category match {
      case SmsCategory.Accepted     => "accepted"
      case SmsCategory.Phishing     => "phishing"
      case SmsCategory.Unsubscribed => "unsubscribed"
    }
    Database.insertSms(sms, urls, detected, status)
  }
}
