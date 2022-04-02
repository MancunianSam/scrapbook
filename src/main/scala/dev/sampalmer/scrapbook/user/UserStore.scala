package dev.sampalmer.scrapbook.user

import cats.MonadError
import cats.data.OptionT
import cats.effect.Async
import cats.implicits._
import dev.sampalmer.scrapbook.routes.LoginRoutes.UsernamePasswordCredentials
import dev.sampalmer.scrapbook.user.UserService.{Role, User}
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux
import tsec.authentication.IdentityStore
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import java.util.UUID

case class UserStore[F[_]](
                            identityStore: IdentityStore[F, UUID, User],
                            checkPassword: UsernamePasswordCredentials => F[Option[User]]
                          )

object UserStore {
  def xa[F[_] : Async]: Aux[F, Unit] = Transactor.fromDriverManager[F](
    "org.postgresql.Driver",
    "jdbc:postgresql:scrapbook",
    "scrapbook",
    "password"
  )

  private def validateUser[F[_] : Async](credentials: UsernamePasswordCredentials, userOpt: Option[User]): F[Option[User]] = {
    userOpt match {
      case Some(user) => BCrypt.checkpwBool[F](credentials.password, PasswordHash(user.password)).map(Option.when(_)(user))
      case None => Async[F].pure(None)
    }
  }

  private def getUser[F[_] : Async](fragment: Fragment)(implicit F: MonadError[F, Throwable]): F[Option[User]] = {
    val user = for {
      userOpt <- fragment
        .query[(String, String, String, String)]
        .to[List]
        .transact(xa)
      user <- F.fromOption(userOpt.headOption, new Exception("User not found"))
      role <- Role.fromReprF[F](user._4)
    } yield {
      val (id, username, password, _) = user
      Option(User(UUID.fromString(id), username, password, role))
    }
    F.attempt(user).map {
      case Left(_) => None
      case Right(value) => value
    }
  }

  def apply[F[_] : Async]()(implicit F: MonadError[F, Throwable]): F[UserStore[F]] = {
    val userStore = new UserStore(
      (id: UUID) => {
        val user: F[Option[User]] = getUser(sql"select * from users where id = ${id.toString}::uuid")
        OptionT(user)
      },
      usr =>
        for {
          user <- getUser(sql"select * from users where username = ${usr.username}")
          validated <- validateUser(usr, user)
        } yield validated

    )
    Async[F].pure(userStore)
  }
}

