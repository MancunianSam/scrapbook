package dev.sampalmer.scrapbook

import cats.data.OptionT
import cats.effect.IO
import dev.sampalmer.scrapbook.auth.AuthConfig
import dev.sampalmer.scrapbook.db.VideoRepository
import dev.sampalmer.scrapbook.routes.VideoRoutes
import dev.sampalmer.scrapbook.server.ScrapbookServer.AppConfig
import dev.sampalmer.scrapbook.service.VideoService
import dev.sampalmer.scrapbook.user.UserService.{Role, User}
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.http4sLiteralsSyntax
import org.mockito.MockitoSugar.{mock, when}
import org.pac4j.core.config.Config
import org.pac4j.core.profile.{CommonProfile, UserProfile}
import org.pac4j.core.util.Pac4jConstants
import org.pac4j.http4s._
import org.pac4j.oidc.profile.OidcProfile

import java.time.Instant
import java.util
import java.util.{Optional, UUID}
import scala.concurrent.duration.DurationInt
import scala.io.Source.fromResource
import scala.jdk.CollectionConverters._

class VideoSpec extends CatsEffectSuite {

  def responseBodyAsString(res: OptionT[IO,Response[IO]]): IO[String] =
    res.value
      .flatMap(resOpt => resOpt.get.body.compile.toList)
      .map(_.map(_.toChar).mkString)

  val sessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = ResponseCookie(_, _, path = Some("/")),
    secret = "ssssssssssssssssssssssssssssssssssssssssssssssssssss",
    maxAge = 5.minutes
  )

  test("get-video returns correct html") {
    val appConfig = AppConfig("", 5432, "", "")
    val request = Request[IO](Method.GET, uri"get-video/e3ad334e-ac4f-47ec-8f3e-f36a79821c6d/")

    val profileMap: util.LinkedHashMap[String, UserProfile] = {
      val map: util.LinkedHashMap[String, UserProfile] = new java.util.LinkedHashMap()
      map.put("test", new OidcProfile())
      map
    }
    val contextBuilder: (Request[IO], Config) => Http4sWebContext[IO] = (_, _) => ({
      val context = mock[Http4sWebContext[IO]]
      when(context.getRequestAttribute(Pac4jConstants.USER_PROFILES)).thenReturn(Optional.of(profileMap))
      context
    })

    ContextRequest[IO](r => r)

    val authConfig = new AuthConfig[IO]("").build()
    val route = VideoRoutes[IO]().routes(VideoService[IO](VideoRepository[IO](appConfig)))
    val authedProtectedPages: HttpRoutes[IO] =
      Session.sessionManagement[IO](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[IO](authConfig, contextBuilder))(route)
    authedProtectedPages.run(request).map(_.status).value.assertEquals(Option(Status.Ok))
  }

  test("get-video redirects to auth provider if the user is not logged in") {
    val appConfig = AppConfig("", 5432, "", "")
    val request = Request[IO](Method.GET, uri"get-video/e3ad334e-ac4f-47ec-8f3e-f36a79821c6d/")
    val profileMap: util.LinkedHashMap[String, UserProfile] = {
      val map: util.LinkedHashMap[String, UserProfile] = new java.util.LinkedHashMap()
      map.put("test", new OidcProfile())
      map
    }
    val contextBuilder: (Request[IO], Config) => Http4sWebContext[IO] = (_, _) => {
      val context = mock[Http4sWebContext[IO]]
      when(context.getRequestAttribute(Pac4jConstants.USER_PROFILES)).thenReturn(Optional.empty())
      context
    }

    val authConfig = new AuthConfig[IO]("").build()
    val route = VideoRoutes[IO]().routes(VideoService[IO](VideoRepository[IO](appConfig)))
    val authedProtectedPages: HttpRoutes[IO] =
      Session.sessionManagement[IO](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[IO](authConfig, contextBuilder))(route)
    authedProtectedPages.run(request).map(_.status).value.assertEquals(Option(Status.Found))
  }
}
