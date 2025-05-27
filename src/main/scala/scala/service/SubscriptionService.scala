package scala.service

import cats.effect.IO

import scala.db.Database

trait SubscriptionService {
  def isSubscribed(recipient: String): IO[Boolean]
  def subscribe(recipient: String): IO[Unit]
  def unsubscribe(recipient: String): IO[Unit]
}

class JdbcSubscriptionService extends SubscriptionService {

  override def isSubscribed(recipient: String): IO[Boolean] = IO {
    Database.isSubscribed(recipient)
  }

  override def subscribe(recipient: String): IO[Unit] = IO {
    Database.updateSubscription(recipient, active = true)
  }

  override def unsubscribe(recipient: String): IO[Unit] = IO {
    Database.updateSubscription(recipient, active = false)
  }
}