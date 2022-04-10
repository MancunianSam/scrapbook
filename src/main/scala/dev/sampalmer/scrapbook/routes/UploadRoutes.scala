package dev.sampalmer.scrapbook.routes

import cats.effect.Async
import cats.implicits._
import dev.sampalmer.presigned.s3.S3
import dev.sampalmer.scrapbook.service.VideoService
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl._
import org.http4s.{AuthedRoutes, EntityDecoder, EntityEncoder, Request}
import org.pac4j.core.context.session.SessionStore
import org.pac4j.core.matching.matcher.csrf.DefaultCsrfTokenGenerator
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.http4s.Http4sWebContext

import java.net.URL
import java.time.Duration
import java.util.UUID

trait UploadRoutes[F[_]] {
  def routes(): AuthedRoutes[List[CommonProfile], F]
}

object UploadRoutes {
  case class Input(id: UUID, filename: String, notes: String)
  case class Response(url: String)

  def apply[F[_] : Async](videoService: VideoService[F], requestToCsrf: Request[F] => String) = new UploadRoutes[F] {
    override def routes(): AuthedRoutes[List[CommonProfile], F] = {
      val dsl = new Http4sDsl[F] {}
      import dsl._

      AuthedRoutes.of[List[CommonProfile], F] {
        case req@GET -> Root / "upload" as _ =>
          Ok(html.upload(requestToCsrf(req.req)))
        case req@POST -> Root / "upload" as _ =>
          implicit val decoder: EntityDecoder[F, Input] = jsonOf[F, Input]
          implicit val encoder: EntityDecoder[F, Response] = jsonOf[F, Response]
          Async[F].flatten {
            for {
              input <- req.req.as[Input]
              _ <- videoService.create(input.id, input.filename, input.notes)
              url <- S3[F]().getPresignedUploadUrl("sam-scrapbook-files", input.id.toString, Duration.ofMinutes(60))
            } yield Ok(Response(url.toString).asJson)
          }
      }
    }
  }
}

