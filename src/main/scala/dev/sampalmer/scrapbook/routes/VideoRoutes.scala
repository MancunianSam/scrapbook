package dev.sampalmer.scrapbook.routes

import cats.{Applicative, MonadThrow}
import cats.data.{EitherT, OptionT}
import cats.effect.Sync
import cats.implicits._
import dev.sampalmer.presigned.s3.S3
import dev.sampalmer.scrapbook.domain.AuthService
import dev.sampalmer.scrapbook.user.UserService.{Role, User}
import dev.sampalmer.scrapbook.video.VideoService
import io.chrisdavenport.fuuid.FUUID
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.http4s.twirl._
import org.http4s.{EntityDecoder, Headers, MediaType, Response, Status}
import play.twirl.api.HtmlFormat
import tsec.authentication._
import tsec.authorization._
import tsec.mac.jca.HMACSHA256

import java.util.UUID

object VideoRoutes {

  def videoRoutes[F[_]: Applicative](videoService: VideoService[F])(implicit entityDecoder: EntityDecoder[F, String], monadThrow: MonadThrow[F]): AuthService[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    TSecAuthService[User, AuthenticatedCookie[HMACSHA256, UUID], F] {
      case req @ GET -> Root / "get-video" / id asAuthed user =>
        for {
          uuid <- FUUID.fromStringF[F](id)
          resp <- videoService.get(user.id ,uuid)
          url <- S3.getPresignedUploadUrl[F]("sam-scrapbook-files", "test")
        } yield {
          Response[F](Status.Ok)
            .withEntity(html.getVideo(url.toString))
            .withHeaders(Headers(`Content-Type`(new MediaType("text", "html"))))
        }
    }
  }
}
