package dev.sampalmer.scrapbook.db

import cats.MonadError
import cats.effect.Async
import cats.implicits._
import io.chrisdavenport.fuuid.FUUID
import dev.sampalmer.scrapbook.db.Db._
import dev.sampalmer.scrapbook.db.VideoRepository.VideoRow
import dev.sampalmer.scrapbook.server.ScrapbookServer.AppConfig
import doobie.implicits._

import java.util.UUID

trait VideoRepository[F[_]] {
  def getVideo(id: FUUID): F[VideoRow]
}

object VideoRepository {
  case class VideoRow(id: String, filename: String)
  def apply[F[_]: Async](config: AppConfig)(implicit F: MonadError[F, Throwable]) = new VideoRepository[F] {
    override def getVideo(id: FUUID): F[VideoRow] = {
      val xa = transactionManager(config.databasePassword, config.databasePort)
      val sql = sql"SELECT id, filename from videos where id = ${id.show}::uuid"
      for {
        videoList: List[VideoRow] <- sql.query[VideoRow]
          .stream
          .transact(xa)
          .compile
          .toList
        video <- F.fromOption(videoList.headOption, new Exception("Video not found"))
      } yield video
    }
  }
}
