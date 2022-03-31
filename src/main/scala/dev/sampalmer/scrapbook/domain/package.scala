package dev.sampalmer.scrapbook

import dev.sampalmer.scrapbook.user.UserService.User
import io.chrisdavenport.fuuid.FUUID
import shapeless.tag
import shapeless.tag.@@
import tsec.authentication.{AuthenticatedCookie, TSecAuthService}
import tsec.mac.jca.HMACSHA256

import java.util.UUID

package object domain {
  type AuthService[F[_]] = TSecAuthService[User, AuthenticatedCookie[HMACSHA256, UUID], F]
}
