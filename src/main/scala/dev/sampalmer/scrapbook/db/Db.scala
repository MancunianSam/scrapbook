package dev.sampalmer.scrapbook.db

import cats.effect.Async
import dev.sampalmer.scrapbook.server.ScrapbookServer.DbConfig
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux

object Db {
  def transactionManager[F[_] : Async](config: DbConfig): Aux[F, Unit] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    s"jdbc:postgresql://localhost:${config.databasePort}/${config.databaseName}",
    config.databaseUser,
    config.databasePassword
  )
}
