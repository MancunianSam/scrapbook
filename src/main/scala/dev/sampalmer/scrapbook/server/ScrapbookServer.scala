package dev.sampalmer.scrapbook.server

import cats.Id
import cats.data.Kleisli
import cats.effect.Sync
import cats.implicits._
import dev.sampalmer.scrapbook.auth.CookieStore
import dev.sampalmer.scrapbook.routes.HomeRoutes.home
import dev.sampalmer.scrapbook.routes.LoginRoutes.{UsernamePasswordCredentials, loginRoute}
import dev.sampalmer.scrapbook.routes.TaskRoutes.taskRoutes
import dev.sampalmer.scrapbook.routes.VideoRoutes.videoRoutes
import dev.sampalmer.scrapbook.tasks.TaskService
import dev.sampalmer.scrapbook.user.UserStore
import dev.sampalmer.scrapbook.user.UserService.{Role, User}
import dev.sampalmer.scrapbook.video.VideoService
import org.http4s.headers.`Content-Type`
import org.http4s.{EntityDecoder, Headers, HttpRoutes, MediaType, Request, Response, Status, UrlForm}
import pureconfig.generic.auto._
import pureconfig.module.catseffect.loadConfigF
import tsec.authentication.{AuthenticatedCookie, BackingStore, SecuredRequest, SecuredRequestHandler, SignedCookieAuthenticator, TSecAuthService, TSecCookieSettings}
import tsec.authorization.BasicRBAC
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import java.util.UUID
import scala.concurrent.duration.DurationInt
import org.http4s.twirl._

object ScrapbookServer {
  case class Config(users: List[UsernamePasswordCredentials])

  def routes[F[_] : Sync](
              userCredentialStore: UserStore[F],
              tokenStore: BackingStore[F, UUID, AuthenticatedCookie[HMACSHA256, UUID]],
              videoService: VideoService[F])
                         (implicit entityDecoder: EntityDecoder[F, String], entityDecoderUser: EntityDecoder[F, UrlForm])

              : Kleisli[F, Request[F], Response[F]] = {
    val settings: TSecCookieSettings = TSecCookieSettings(
      cookieName = "tsec-auth",
      secure = false,
      expiryDuration = 10.minutes, // Absolute expiration time
      maxIdle = None // Rolling window expiration. Set this to a Finiteduration if you intend to have one
    )

    val key: MacSigningKey[HMACSHA256] = HMACSHA256.generateKey[Id]

    val authenticator = SignedCookieAuthenticator[F, UUID, User, HMACSHA256](
      settings,
      tokenStore,
      userCredentialStore.identityStore,
      key
      )
    val unauthorisedPage = Response[F](Status.Unauthorized)
      .withEntity(html.unauthorised("Not logged in error"))
      .withHeaders(Headers(`Content-Type`(new MediaType("text", "html"))))

    def unauthorised: Request[F] => F[Response[F]] = _ => Sync[F].pure(unauthorisedPage)

    val secureRoutes: HttpRoutes[F] = SecuredRequestHandler(authenticator).liftService(
      videoRoutes(VideoService()) <+> taskRoutes(TaskService()), unauthorised
    )
    val loginRoutes = loginRoute(authenticator, userCredentialStore.checkPassword)


    (home <+> loginRoutes <+> secureRoutes).orNotFound
  }

  def app[F[_] : Sync]()(implicit entityDecoder: EntityDecoder[F, String], edu: EntityDecoder[F, UrlForm]): F[Kleisli[F, Request[F], Response[F]]] = {
    for {
      config <- loadConfigF[F, Config]
      userCredentialStore <- UserStore[F](config.users.head, config.users.tail: _*)
      finalHttpApp = routes[F](userCredentialStore, CookieStore.empty, VideoService())
    } yield finalHttpApp
  }
}
