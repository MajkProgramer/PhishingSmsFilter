package scala.config

import pureconfig._
import pureconfig.generic.auto._

case class DbConfig(url: String, user: String, password: String)
case class AppConfig(subscriptionPhone: String, token: String, isProdEnv: Boolean, db: DbConfig)

object AppConfig {
  def load: AppConfig =
    ConfigSource.default.at("app").loadOrThrow[AppConfig]
}