package dev.sampalmer.scrapbook

import cats.data.OptionT
import cats.effect.IO
import dev.sampalmer.scrapbook.routes.VideoRoutes
import dev.sampalmer.scrapbook.service.VideoService
import dev.sampalmer.scrapbook.user.UserService.{Role, User}
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._
import tsec.authentication.{AuthenticatedCookie, SecuredRequest, TSecCookieSettings}
import tsec.cookies.SignedCookie
import tsec.mac.jca.HMACSHA256

import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.io.Source.fromResource

class VideoSpec extends CatsEffectSuite {

  def getSecuredRequest(uri: Uri, method: Method = Method.GET, role: Role = Role.Creator): SecuredRequest[IO, User, AuthenticatedCookie[HMACSHA256, UUID]] = {
    val request = Request[IO](method, uri)
    val cookieContent = SignedCookie[HMACSHA256]("")
    val authenticatedCookie = AuthenticatedCookie
      .build[HMACSHA256, UUID](UUID.randomUUID(), cookieContent, UUID.randomUUID(), Instant.now().minusSeconds(1000), None, TSecCookieSettings(secure = true, expiryDuration = 10.minutes, maxIdle = None))
    val user = User(UUID.randomUUID(), "username", "password", role)
    SecuredRequest[IO, User, AuthenticatedCookie[HMACSHA256, UUID]](request, user, authenticatedCookie)
  }

  def responseBodyAsString(res: OptionT[IO,Response[IO]]): IO[String] =
    res.value
      .flatMap(resOpt => resOpt.get.body.compile.toList)
      .map(_.map(_.toChar).mkString)

  test("get-video returns correct html") {
    val securedRequest = getSecuredRequest(uri"get-video/55995ee7-e057-48e0-a8d7-13403d1a2609")
    val res: OptionT[IO, Response[IO]] = VideoRoutes.videoRoutes(VideoService[IO]()).run(securedRequest)
    val bodyAsString = responseBodyAsString(res)
    bodyAsString.assertEquals(fromResource("html/video.html").mkString)
  }

  test("get-video returns unauthorised for the incorrect role") {
    val securedRequest = getSecuredRequest(uri"get-video/55995ee7-e057-48e0-a8d7-13403d1a2609", role = Role.Administrator)
    val res: OptionT[IO, Response[IO]] = VideoRoutes.videoRoutes(VideoService[IO]()).run(securedRequest)
    res.value.map(_.get.status.code).assertEquals(401)
  }
}
