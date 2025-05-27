package scala.db

import java.sql.{Connection, DriverManager}
import scala.config.AppConfig
import scala.model.Sms

object Database {
  private val cfg = AppConfig.load

  private def getConnection: Connection = {
    Class.forName("org.postgresql.Driver")
    DriverManager.getConnection(cfg.db.url, cfg.db.user, cfg.db.password)
  }

  def init(): Unit = {
    val conn = getConnection
    val stmt = conn.createStatement()

    stmt.executeUpdate(
      """
        |CREATE TABLE IF NOT EXISTS subscriptions (
        |  recipient VARCHAR PRIMARY KEY,
        |  active BOOLEAN NOT NULL
        |);
        |""".stripMargin)

    stmt.executeUpdate(
      """
        |CREATE TABLE IF NOT EXISTS sms (
        |  id SERIAL PRIMARY KEY,
        |  sender VARCHAR NOT NULL,
        |  recipient VARCHAR NOT NULL,
        |  message TEXT NOT NULL,
        |  urls TEXT[],
        |  detected_phishing BOOLEAN NOT NULL,
        |  received_at TIMESTAMP NOT NULL DEFAULT NOW(),
        |  status VARCHAR NOT NULL
        |);
        |""".stripMargin)

    stmt.close()
    conn.close()
  }

  def insertSms(sms: Sms, urls: List[String], detectedPhishing: Boolean, status: String): Unit = {
    val conn = getConnection
    val stmt = conn.prepareStatement(
      "INSERT INTO sms (sender, recipient, message, urls, detected_phishing, status) VALUES (?, ?, ?, ?, ?, ?)"
    )
    stmt.setString(1, sms.sender)
    stmt.setString(2, sms.recipient)
    stmt.setString(3, sms.message)
    val urlArray = conn.createArrayOf("text", urls.toArray)
    stmt.setArray(4, urlArray)
    stmt.setBoolean(5, detectedPhishing)
    stmt.setString(6, status)

    stmt.executeUpdate()
    stmt.close()
    conn.close()
  }

  def isSubscribed(recipient: String): Boolean = {
    val conn = getConnection
    val stmt = conn.prepareStatement(
      "SELECT active FROM subscriptions WHERE recipient = ?"
    )
    stmt.setString(1, recipient)
    val rs = stmt.executeQuery()
    val subscribed = if (rs.next()) rs.getBoolean("active") else false
    rs.close()
    stmt.close()
    conn.close()
    subscribed
  }

  def updateSubscription(recipient: String, active: Boolean): Unit = {
    val conn = getConnection
    val upsert =
      """
        |INSERT INTO subscriptions (recipient, active)
        |VALUES (?, ?)
        |ON CONFLICT (recipient)
        |DO UPDATE SET active = EXCLUDED.active;
        |""".stripMargin

    val stmt = conn.prepareStatement(upsert)
    stmt.setString(1, recipient)
    stmt.setBoolean(2, active)
    stmt.executeUpdate()
    stmt.close()
    conn.close()
  }
}