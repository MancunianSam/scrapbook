package dev.sampalmer.scrapbook.user

import cats.Applicative
import cats.data.OptionT
import cats.effect.{Ref, Sync}
import dev.sampalmer.scrapbook.routes.LoginRoutes.UsernamePasswordCredentials
import dev.sampalmer.scrapbook.user.UserService.User
import tsec.authentication.AuthenticatedCookie
import tsec.mac.jca.HMACSHA256

import java.util.UUID
import cats.implicits._
import dev.sampalmer.scrapbook.user.UserService.User
import io.chrisdavenport.fuuid.FUUID
import tsec.authentication.IdentityStore
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

case class UserStore[F[_]](
                      identityStore: IdentityStore[F, UUID, User],
                      checkPassword: UsernamePasswordCredentials => F[Option[User]]
                    )

object UserStore {
  def newUser[F[_]: Sync](username: String, password: String): F[User] = {
    Applicative[F].map(
      BCrypt.hashpw[F](password)
    )(User(UUID.randomUUID(), username, _))
  }

  private def validateUser[F[_]: Sync](credentials: UsernamePasswordCredentials)(
    users: List[User]): F[Option[User]] =
    users.findM(
      user =>
        BCrypt
          .checkpwBool[F](credentials.password, user.password)
          .map(_ && credentials.username == user.username),
    )

  def apply[F[_]: Sync](user: UsernamePasswordCredentials, users: UsernamePasswordCredentials*): F[UserStore[F]] =
    for {
      userList <- (user +: users)
        .map(u => UserStore.newUser(u.username, u.password))
        .toList
        .sequence
      users <- Ref.of[F, Map[UUID, User]](userList.map(u => u.id -> u).toMap)
    } yield
      new UserStore(
        (id: UUID) => OptionT(users.get.map(_.get(id))),
        usr => users.get.map(_.values.toList).flatMap(validateUser(usr)(_))
      )

}

