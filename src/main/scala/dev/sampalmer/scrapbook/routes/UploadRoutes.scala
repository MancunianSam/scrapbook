package dev.sampalmer.scrapbook.routes

import cats.MonadThrow
import cats.effect.Async
import cats.implicits._
import dev.sampalmer.scrapbook.service.UploadService
import dev.sampalmer.scrapbook.utils.Utils.response
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl._
import org.http4s.{AuthedRoutes, EntityDecoder, HttpRoutes}
import org.pac4j.core.profile.CommonProfile

trait UploadRoutes[F[_]] {
  def routes(uploadService: UploadService[F]): AuthedRoutes[List[CommonProfile], F]
}

object UploadRoutes {
  def apply[F[_] : Async]() = new UploadRoutes[F] {
    override def routes(uploadService: UploadService[F]): AuthedRoutes[List[CommonProfile], F] = {
      val dsl = new Http4sDsl[F] {}
      import dsl._
      AuthedRoutes.of[List[CommonProfile], F] {
        case GET -> Root / "upload" as profiles =>
          Async[F].flatten {
            for {
              url <- uploadService.uploadUrl()
            } yield {
              Ok(html.upload(url.toString))
            }
          }
      }
    }
  }
}

