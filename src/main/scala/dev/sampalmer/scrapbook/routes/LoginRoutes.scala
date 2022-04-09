package dev.sampalmer.scrapbook.routes

import cats.effect.{Async, Sync}
import dev.sampalmer.scrapbook.auth.AuthConfig
import dev.sampalmer.scrapbook.server.ScrapbookServer.AppConfig
import org.http4s.{HttpRoutes, Request}
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl._
import org.pac4j.core.config.Config
import org.pac4j.http4s.{CallbackService, Http4sWebContext, LogoutService, SecurityFilterMiddleware, Session}

trait LoginRoutes[F[_]] {
  def routes(): HttpRoutes[F]
}

object LoginRoutes {

  def apply[F[_] <: AnyRef : Async](authConfig: Config, contextBuilder: (Request[F], Config) => Http4sWebContext[F]) = new LoginRoutes[F] {
    val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    val localLogoutService = new LogoutService[F](authConfig, contextBuilder, Some("/"), destroySession = true)

    val callbackService = new CallbackService[F](authConfig, contextBuilder)

    override def routes(): HttpRoutes[F] =
      HttpRoutes.of[F] {
        case req@GET -> Root / "callback" =>
          callbackService.callback(req)
        case req@POST -> Root / "callback" =>
          callbackService.callback(req)
        case req@GET -> Root / "logout" =>
          localLogoutService.logout(req)
      }
  }

}
