package dev.sampalmer.scrapbook

import cats.Id
import cats.data.OptionT
import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import dev.sampalmer.scrapbook.auth.CookieStore
import dev.sampalmer.scrapbook.routes.LoginRoutes.loginRoute
import dev.sampalmer.scrapbook.user.UserService.{Role, User}
import fs2.Stream
import org.http4s.headers._
import munit.CatsEffectSuite
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{EntityBody, MediaType, Method, Request, Response, UrlForm}
import tsec.authentication.{BackingStore, IdentityStore, SignedCookieAuthenticator, TSecCookieSettings}
import tsec.mac.jca.{HMACSHA256, MacSigningKey}

import java.util.UUID
import scala.concurrent.duration.DurationInt

class LoginSpec extends CatsEffectSuite {
  val user: UUID => User = User(_, "", "", Role.Creator)

  def authenticator: SignedCookieAuthenticator[IO, UUID, User, HMACSHA256] = {
    val identityStore: IdentityStore[IO, UUID, User] = new BackingStore[IO, UUID, User]() {
      override def put(elem: User): IO[User] = IO(user(UUID.randomUUID()))

      override def update(v: User): IO[User] = IO(user(UUID.randomUUID()))

      override def delete(id: UUID): IO[Unit] = IO.unit

      override def get(id: UUID): OptionT[IO, User] = OptionT(IO(user(UUID.randomUUID()).some))
    }
    val settings: TSecCookieSettings = TSecCookieSettings(
      cookieName = "tsec-auth",
      secure = false,
      expiryDuration = 10.minutes, // Absolute expiration time
      maxIdle = None // Rolling window expiration. Set this to a Finiteduration if you intend to have one
    )

    val key: MacSigningKey[HMACSHA256] = HMACSHA256.generateKey[Id]
    SignedCookieAuthenticator[IO, UUID, User, HMACSHA256](
      settings,
      CookieStore.empty,
      identityStore,
      key
    )
  }

  test("login redirects on successful login") {
    val request = Request[IO](Method.POST, uri"/login")
      .withContentType(`Content-Type`(new MediaType("application", "x-www-form-urlencoded")))
      .withEntity(UrlForm(("username", "username"), ("password", "password")))
    val res: OptionT[IO, Response[IO]] = loginRoute[IO](authenticator, _ => IO(user(UUID.randomUUID()).some)).run(request)
    res.value.map(_.get.status.code).assertEquals(302)
  }

  test("login returns unauthorised if user validation fails") {
    val request = Request[IO](Method.POST, uri"/login")
      .withContentType(`Content-Type`(new MediaType("application", "x-www-form-urlencoded")))
      .withEntity(UrlForm(("username", "username"), ("password", "password")))
    val res: OptionT[IO, Response[IO]] = loginRoute[IO](authenticator, _ => IO(None)).run(request)
    res.value.map(_.get.status.code).assertEquals(401)
  }

  test("login returns an error if a form element is missing") {
    val request = Request[IO](Method.POST, uri"/login")
      .withContentType(`Content-Type`(new MediaType("application", "x-www-form-urlencoded")))
      .withEntity(UrlForm(("username", "username")))
    val res: OptionT[IO, Response[IO]] = loginRoute[IO](authenticator, _ => IO(None)).run(request)
    val exceptionMessage: IO[String] = interceptIO[Exception](res.value).map(_.getMessage)
    exceptionMessage.assertEquals("Missing username or password")
  }
}
