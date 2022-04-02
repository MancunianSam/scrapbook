package dev.sampalmer.scrapbook.routes

import cats.implicits._
import cats.{Applicative, MonadThrow}
import dev.sampalmer.scrapbook.domain.AuthService
import dev.sampalmer.scrapbook.service.UploadService
import dev.sampalmer.scrapbook.user.UserService.{Role, User}
import dev.sampalmer.scrapbook.utils.Utils.response
import org.http4s.EntityDecoder
import org.http4s.dsl.Http4sDsl
import tsec.authentication.{AuthenticatedCookie, TSecAuthService, asAuthed}
import tsec.authorization.BasicRBAC
import tsec.mac.jca.HMACSHA256

import java.util.UUID

object UploadRoutes {
  def uploadRoutes[F[_] : Applicative](uploadService: UploadService[F])(implicit entityDecoder: EntityDecoder[F, String], monadThrow: MonadThrow[F]): AuthService[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    val basicRBAC = BasicRBAC[F, Role, User, AuthenticatedCookie[HMACSHA256, UUID]](Role.Creator)
    TSecAuthService.withAuthorization[User, AuthenticatedCookie[HMACSHA256, UUID], F](basicRBAC)({
      case GET -> Root / "upload" asAuthed user =>
        for {
          url <- uploadService.uploadUrl()
        } yield response[F](html.upload(url.toString))
    })
  }
}
