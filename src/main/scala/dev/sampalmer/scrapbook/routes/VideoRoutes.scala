package dev.sampalmer.scrapbook.routes

import cats.effect.Async
import cats.implicits._
import dev.sampalmer.scrapbook.service.VideoService
import dev.sampalmer.scrapbook.service.VideoService.Video
import io.chrisdavenport.fuuid.FUUID
import org.http4s.{AuthedRoutes, Method}
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl._
import org.pac4j.core.profile.CommonProfile

import java.util.UUID
trait VideoRoutes[F[_]] {
  def routes(videoService: VideoService[F]): AuthedRoutes[List[CommonProfile], F]
}

object VideoRoutes {
  def apply[F[_] : Async](): VideoRoutes[F] = (videoService: VideoService[F]) => {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    AuthedRoutes.of[List[CommonProfile], F] {
      case req@GET -> Root / "get-video" / id as profiles =>
        Async[F].flatten {
          for {
            uuid <- FUUID.fromStringF[F](id)
            resp <- videoService.get(UUID.randomUUID(), uuid)
          } yield Ok(html.getVideo(resp))
        }
      case req =>
        Ok(html.getVideo(Video("", "")))
    }
  }
}
