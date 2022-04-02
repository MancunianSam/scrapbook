package dev.sampalmer.scrapbook.service

import cats.Applicative
import cats.effect.kernel.Sync
import dev.sampalmer.presigned.s3.S3
import io.chrisdavenport.fuuid.FUUID

import java.net.URL
import java.util.UUID

trait VideoService[F[_]] {
  def get(userId: UUID, id: FUUID): F[String]
}

object VideoService {
  def apply[F[_] : Sync](): VideoService[F] = new VideoService[F] {
    override def get(userId: UUID, id: FUUID): F[String] = {
      Applicative[F].pure("")
    }
  }
}
