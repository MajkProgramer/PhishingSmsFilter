package scala.main

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._

import scala.api.SmsRoutes
import scala.db.Database
import scala.service.{JdbcSubscriptionService, PostgresSmsStorage, SmsStorage, SubscriptionService}

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    IO(Database.init()).flatMap { _ =>
      EmberClientBuilder.default[IO].build.use { client =>
        val subs: SubscriptionService = new JdbcSubscriptionService()
        val storage: SmsStorage = new PostgresSmsStorage()

        val httpApp = SmsRoutes.routes(client, subs, storage).orNotFound

        EmberServerBuilder.default[IO]
          .withHostOption(Host.fromString("0.0.0.0"))
          .withPort(Port.fromInt(8080).get)
          .withHttpApp(httpApp)
          .build
          .use(_ => IO.never)
          .as(ExitCode.Success)
      }
    }
  }
}