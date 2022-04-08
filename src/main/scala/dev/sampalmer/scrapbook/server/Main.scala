package dev.sampalmer.scrapbook.server

import cats.effect.std.Dispatcher
import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.IpLiteralSyntax
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import org.pac4j.http4s.Http4sWebContext

object Main extends IOApp {
  def run(args: List[String]) = {
    Dispatcher[IO].use(d => {
      for {
        app <- ScrapbookServer.app[IO](d)
        loggingMiddleware = Logger.httpApp[IO](logHeaders = true, logBody = true)(app)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(loggingMiddleware)
          .build
          .use(_ => IO.never)
          .as(ExitCode.Success)
      } yield server
    })
  }
}
