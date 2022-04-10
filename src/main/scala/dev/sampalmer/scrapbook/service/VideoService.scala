package dev.sampalmer.scrapbook.service

import cats.effect.Async
import cats.implicits._
import dev.sampalmer.presigned.cloudfront.Cloudfront
import dev.sampalmer.scrapbook.db.VideoRepository
import dev.sampalmer.scrapbook.db.VideoRepository.VideoRow
import dev.sampalmer.scrapbook.server.ScrapbookServer.AppConfig
import dev.sampalmer.scrapbook.service.VideoService.Video
import io.chrisdavenport.fuuid.FUUID

import java.util.UUID

trait VideoService[F[_]] {
  def get(userId: UUID, id: FUUID): F[Video]
  def create(id: UUID, filename: String, notes: String): F[Int]
}

object VideoService {
  val cloudfrontUrl = "https://d14agbynhxhme0.cloudfront.net"
  case class Video(url: String, name: String)

  def apply[F[_] : Async](appConfig: AppConfig): VideoService[F] = new VideoService[F] {
    val videoRepository: VideoRepository[F] = VideoRepository[F](appConfig)
    override def get(userId: UUID, id: FUUID): F[Video] = for {
      videoRow <- videoRepository.getVideo(id)
      url <- Cloudfront[F](appConfig.signingConfig.privateKey, appConfig.signingConfig.keyId).getSignedUrl(s"$cloudfrontUrl/${id.show}", 3600)
    } yield Video(url, videoRow.filename)

    override def create(id: UUID, filename: String, notes: String): F[Int] =
      videoRepository.createVideo(id, filename, notes)
  }
}
