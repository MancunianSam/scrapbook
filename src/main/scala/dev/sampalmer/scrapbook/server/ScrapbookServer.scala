package dev.sampalmer.scrapbook.server

import cats.data.{Kleisli, OptionT}
import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.implicits._
import ciris.{ConfigValue, env}
import dev.sampalmer.scrapbook.auth.AuthConfig
import dev.sampalmer.scrapbook.db.VideoRepository
import dev.sampalmer.scrapbook.routes.{LoginRoutes, UploadRoutes, VideoRoutes}
import dev.sampalmer.scrapbook.service.{UploadService, VideoService}
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.Location
import org.http4s.implicits.{http4sKleisliResponseSyntaxOptionT, http4sLiteralsSyntax}
import org.http4s.server.Router
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http4s._

import scala.concurrent.duration.DurationInt

object ScrapbookServer {
  case class AppConfig(databasePassword: String, databasePort: Int, cookieSecret: String, clientSecret: String)

  def routes[F[_] <: AnyRef : Async](contextBuilder: (Request[F], Config) => Http4sWebContext[F],
                                     videoService: VideoService[F],
                                     uploadService: UploadService[F],
                                     sessionConfig: SessionConfig,
                                     authConfig: Config): Kleisli[F, Request[F], Response[F]] = {
    val uploadRoutes: UploadRoutes[F] = UploadRoutes[F]()
    val videoRoutes = VideoRoutes[F]()
    val allRoutes = videoRoutes.routes(videoService) <+> uploadRoutes.routes(uploadService)
    val root = LoginRoutes[F](authConfig, contextBuilder).routes()
    val authedTrivial: AuthedRoutes[List[CommonProfile], F] =
      Kleisli(_ => {
        val dsl: Http4sDsl[F] = new Http4sDsl[F]{}
        import dsl._
        OptionT.liftF(Found(Location(uri"/")))
      })

    val loginPages: HttpRoutes[F] =
      Router(
        "oidc"     -> Session.sessionManagement[F](sessionConfig)
          .compose(SecurityFilterMiddleware.securityFilter[F](authConfig, contextBuilder, Some("OidcClient"))).apply(authedTrivial)
      )

    val authedProtectedPages: HttpRoutes[F] =
      Session.sessionManagement[F](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[F](authConfig, contextBuilder))(allRoutes)

    Router(
      "/login" -> (Session.sessionManagement[F](sessionConfig) _){ loginPages },
      "/protected" -> authedProtectedPages,
      "/" -> (Session.sessionManagement[F](sessionConfig) _) (root)
    ).orNotFound
  }

  def config[F[_] : Async]: ConfigValue[F, AppConfig] =
    (
      env("DATABASE_PASSWORD").as[String],
      env("COOKIE_SECRET").as[String],
      env("CLIENT_SECRET").as[String]
      ).parMapN({
      (password, cookieSecret, clientSecret) => AppConfig(password, 5432, cookieSecret, clientSecret)
    })

  def getSessionConfig(cookieSecret: String): SessionConfig = {
    SessionConfig(
      cookieName = "session",
      mkCookie = ResponseCookie(_, _, path = Some("/")),
      secret = cookieSecret,
      maxAge = 5.minutes
    )
  }

  def app[F[_] <: AnyRef : Async](dispatcher: Dispatcher[F]): F[Kleisli[F, Request[F], Response[F]]] = {
    for {
      conf <- config.load[F]
      authConfig <- AuthConfig[F](conf.clientSecret)
      allRoutes = routes[F](
        Http4sWebContext.withDispatcherInstance[F](dispatcher),
        VideoService[F](VideoRepository(conf)), UploadService[F](),
        getSessionConfig(conf.cookieSecret),
        authConfig.build())
      finalHttpApp <- Async[F].pure(allRoutes)
    } yield finalHttpApp
  }
}
