package dev.sampalmer.scrapbook.service

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import dev.sampalmer.presigned.s3.S3
import io.chrisdavenport.fuuid.FUUID

import java.net.URL
import java.time.Duration

trait UploadService[F[_]] {
  def uploadUrl(): F[URL]
}

object UploadService {
  def apply[F[_] : Applicative]()(implicit sync: Sync[F]) = new UploadService[F] {
    override def uploadUrl(): F[URL] =
      for {
        uuid <- FUUID.randomFUUID[F]
        url <- S3[F]().getPresignedUploadUrl("sam-scrapbook-files", uuid.toString, Duration.ofMinutes(60))
      } yield url
  }
}