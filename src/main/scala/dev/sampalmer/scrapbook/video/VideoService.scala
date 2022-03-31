package dev.sampalmer.scrapbook.video

import cats.effect.kernel.Sync
import io.chrisdavenport.fuuid.FUUID

import java.util.UUID

trait VideoService[F[_]] {
 def get(userId: UUID, id: FUUID): F[String]
}
object VideoService {
  def apply[F[_]: Sync](): VideoService[F] = new VideoService[F] {
    override def get(userId: UUID, id: FUUID): F[String] = {
      Sync[F].pure("test")
    }
  }
}
