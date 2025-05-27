package scala.model

sealed trait SmsCategory
object SmsCategory {
  case object Accepted extends SmsCategory
  case object Phishing extends SmsCategory
  case object Unsubscribed extends SmsCategory
}
