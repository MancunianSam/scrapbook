package dev.sampalmer.scrapbook.user

import cats.{Applicative, Eq, MonadError}
import dev.sampalmer.scrapbook.user.UserService.User
import io.chrisdavenport.fuuid.FUUID
import org.http4s.circe.jsonEncoderOf
import org.http4s.{EntityEncoder, Request, Response}
import tsec.authentication.{AuthenticatedCookie, SecuredRequest}
import tsec.authorization.{AuthGroup, AuthorizationInfo, SimpleAuthEnum}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import java.util.UUID

object UserService {
  type PartialResponse[F[_]] = PartialFunction[SecuredRequest[F, User, AuthenticatedCookie[HMACSHA256, UUID]], F[Response[F]]]
  type PartialFrom[F[_], Req] = PartialFunction[Req, F[Response[F]]]
  type Secure[F[_]] = SecuredRequest[F, User, AuthenticatedCookie[HMACSHA256, UUID]]

  implicit def jsonStringListEncoder[F[_]]: EntityEncoder[F, List[String]] = jsonEncoderOf[F, List[String]]

  sealed case class Role(roleRepr: String)

  object Role extends SimpleAuthEnum[Role, String] {

    val Administrator: Role = Role("Administrator")
    val Creator: Role      = Role("Creator")
    val Viewer: Role        = Role("Viewer")

    implicit val E: Eq[Role] = Eq.fromUniversalEquals[Role]

    def getRepr(t: Role): String = t.roleRepr

    protected val values: AuthGroup[Role] = AuthGroup(Administrator, Creator, Viewer)
  }
  case class User(id: UUID, username: String, password: String, role: Role = Role.Creator)
  object User {
    implicit def authRole[F[_]](implicit F: MonadError[F, Throwable]): AuthorizationInfo[F, Role, User] =
      (u: User) => F.pure(u.role)
  }

}
