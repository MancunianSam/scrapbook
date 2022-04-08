package dev.sampalmer.scrapbook

import cats.data.OptionT
import cats.effect.IO
import dev.sampalmer.scrapbook.auth.AuthConfig
import dev.sampalmer.scrapbook.routes.LoginRoutes
import dev.sampalmer.scrapbook.server.ScrapbookServer.AppConfig
import dev.sampalmer.scrapbook.user.UserService.User
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.pac4j.core.config.Config
import org.pac4j.http4s.Http4sWebContext

import java.util.UUID

class LoginSpec extends CatsEffectSuite {


  test("login redirects on successful login") {
    val request = Request[IO](Method.POST, uri"/callback")
      .withContentType(`Content-Type`(new MediaType("application", "x-www-form-urlencoded")))
      .withEntity(UrlForm(("username", "username"), ("password", "password")))
    val contextBuilder: (Request[IO], Config) => Http4sWebContext[IO] = (req, conf) => Http4sWebContext.ioInstance(req, conf)
    val authConfig = new AuthConfig[IO]("appConfig").build()
    val res: OptionT[IO, Response[IO]] = LoginRoutes[IO](authConfig, contextBuilder).routes().run(request)
    res.value.map(_.get.status.code).assertEquals(302)
  }
//
//  test("login returns unauthorised if user validation fails") {
//    val request = Request[IO](Method.POST, uri"/login")
//      .withContentType(`Content-Type`(new MediaType("application", "x-www-form-urlencoded")))
//      .withEntity(UrlForm(("username", "username"), ("password", "password")))
//    val res: OptionT[IO, Response[IO]] = loginRoute[IO](authenticator, _ => IO(None)).run(request)
//    res.value.map(_.get.status.code).assertEquals(401)
//  }
//
//  test("login returns an error if a form element is missing") {
//    val request = Request[IO](Method.POST, uri"/login")
//      .withContentType(`Content-Type`(new MediaType("application", "x-www-form-urlencoded")))
//      .withEntity(UrlForm(("username", "username")))
//    val res: OptionT[IO, Response[IO]] = loginRoute[IO](authenticator, _ => IO(None)).run(request)
//    val exceptionMessage: IO[String] = interceptIO[Exception](res.value).map(_.getMessage)
//    exceptionMessage.assertEquals("Missing username or password")
//  }
}
