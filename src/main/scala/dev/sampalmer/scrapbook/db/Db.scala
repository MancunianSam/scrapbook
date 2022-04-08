package dev.sampalmer.scrapbook.db

import cats.effect.Async
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux

object Db {
  def transactionManager[F[_] : Async](databasePassword: String, databasePort: Int): Aux[F, Unit] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    s"jdbc:postgresql://localhost:$databasePort/scrapbook",
    "scrapbook",
    databasePassword
  )
}
