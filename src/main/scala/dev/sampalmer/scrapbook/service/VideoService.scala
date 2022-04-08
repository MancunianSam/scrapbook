package dev.sampalmer.scrapbook.service

import cats.effect.Async
import cats.implicits._
import dev.sampalmer.presigned.cloudfront.Cloudfront
import dev.sampalmer.scrapbook.db.VideoRepository
import dev.sampalmer.scrapbook.service.VideoService.Video
import io.chrisdavenport.fuuid.FUUID

import java.util.UUID

trait VideoService[F[_]] {
  def get(userId: UUID, id: FUUID): F[Video]
}

object VideoService {
  val cloudfrontUrl = "https://d14agbynhxhme0.cloudfront.net"
  case class Video(url: String, name: String)

  def apply[F[_] : Async](videoRepository: VideoRepository[F]): VideoService[F] = new VideoService[F] {
    override def get(userId: UUID, id: FUUID): F[Video] = for {
      videoRow <- videoRepository.getVideo(id)
      url <- Cloudfront.getSignedUrl[F](s"$cloudfrontUrl/${id.show}", 3600)
    } yield Video(url, videoRow.filename)
  }
}
