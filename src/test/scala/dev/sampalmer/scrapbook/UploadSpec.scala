package dev.sampalmer.scrapbook

import cats.data.OptionT
import cats.effect.IO
import com.dimafeng.testcontainers.munit.TestContainerForAll
import com.dimafeng.testcontainers.{ContainerDef, PostgreSQLContainer}
import dev.sampalmer.scrapbook.auth.AuthConfig
import dev.sampalmer.scrapbook.db.Db.transactionManager
import dev.sampalmer.scrapbook.db.VideoRepository
import dev.sampalmer.scrapbook.routes.UploadRoutes
import dev.sampalmer.scrapbook.routes.UploadRoutes.Input
import dev.sampalmer.scrapbook.server.ScrapbookServer.{AppConfig, AuthSecretConfig, DbConfig, SigningConfig}
import dev.sampalmer.scrapbook.service.VideoService
import doobie.implicits._
import io.chrisdavenport.fuuid.FUUID
import io.circe.Printer
import io.circe.syntax._
import io.circe.generic.auto._
import munit.CatsEffectSuite
import org.http4s.implicits._
import org.http4s.{ContextRequest, HttpRoutes, Method, Request, Response, ResponseCookie, Status, Uri}
import org.mockito.MockitoSugar.mock
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http4s.{Http4sWebContext, SecurityFilterMiddleware, Session, SessionConfig}

import java.util.UUID
import scala.concurrent.duration._

class UploadSpec extends CatsEffectSuite with TestContainerForAll {
  def responseBodyAsString(res: OptionT[IO, Response[IO]]): IO[String] =
    res.value
      .flatMap(resOpt => resOpt.get.body.compile.toList)
      .map(_.map(_.toChar).mkString)

  val sessionConfig: SessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = ResponseCookie(_, _, path = Some("/")),
    secret = "testtesttesttest",
    maxAge = 5.minutes
  )

  override def afterContainersStart(containers: containerDef.Container): Unit = {
    containers match {
      case postgres: PostgreSQLContainer =>
        val dbConfig = DbConfig(postgres.databaseName, postgres.username, postgres.password, postgres.mappedPort(5432))
        val xa = transactionManager[IO](dbConfig)
        val create = sql"create table videos (id uuid, filename varchar(255), notes varchar(1000))".update.run
        create.transact(xa).unsafeRunSync()
    }
    super.afterContainersStart(containers)
  }

  test("upload get returns the correct html") {
    withContainers {
      case postgres: PostgreSQLContainer =>
        val appConfig: AppConfig = getAppConfig(postgres)
        val request = Request[IO](Method.GET, uri"/upload")
        val csrf = UUID.randomUUID().toString

        val route = UploadRoutes[IO](VideoService[IO](appConfig), _ => csrf).routes()
        val contextRequest = ContextRequest(List[CommonProfile](), request)
        val body = responseBodyAsString(route.run(contextRequest))

        body.map(_.contains("<input type=\"file\" id=\"file\">")).assertEquals(true)
        body.map(_.contains("<button id=\"click\">Click</button>")).assertEquals(true)
        body.map(_.contains(s"<input type=\"hidden\" value=\"$csrf\">")).assertEquals(true)
    }
  }

  test("upload post creates a new video") {
    withContainers {
      case postgres: PostgreSQLContainer =>
        val id = UUID.fromString("dd6d64e8-1e3f-48d2-8ef3-69b64d46721c")
        val input = Input(id, "filename", "notes").asJson.printWith(Printer.noSpaces).getBytes()
        val csrf = UUID.randomUUID().toString
        val url = s"/upload?pac4jCsrfToken=$csrf"
        val request = Request[IO](Method.POST, Uri.unsafeFromString(url))
          .withBodyStream(fs2.Stream.emits(input))
        val appConfig = getAppConfig(postgres)
        val route = UploadRoutes[IO](VideoService[IO](appConfig), _ => csrf).routes()
        val contextRequest = ContextRequest(List[CommonProfile](), request)
        route.run(contextRequest).value.unsafeRunSync()
        val video = VideoRepository[IO](appConfig).getVideo(FUUID.fromUUID(id)).unsafeRunSync()
        assertEquals(video.id, id.toString)
        assertEquals(video.filename, "filename")
        assertEquals(video.notes, "notes")
    }
  }

  test("upload post returns an upload url") {
    withContainers {
      case postgres: PostgreSQLContainer =>
        val id = UUID.fromString("dd6d64e8-1e3f-48d2-8ef3-69b64d46721c")
        val input = Input(id, "filename", "notes").asJson.printWith(Printer.noSpaces).getBytes()
        val csrf = UUID.randomUUID().toString
        val url = s"/upload?pac4jCsrfToken=$csrf"
        val request = Request[IO](Method.POST, Uri.unsafeFromString(url))
          .withBodyStream(fs2.Stream.emits(input))
        val appConfig = getAppConfig(postgres)
        val route = UploadRoutes[IO](VideoService[IO](appConfig), _ => csrf).routes()
        val contextRequest = ContextRequest(List[CommonProfile](), request)
        val response = route.run(contextRequest)
        responseBodyAsString(response).map(_.contains(s"https://sam-scrapbook-files.s3.eu-west-2.amazonaws.com/$id")).assertEquals(true)
    }
  }

  private def getAppConfig(postgres: PostgreSQLContainer) = {
    val dbConfig = DbConfig(postgres.databaseName, postgres.username, postgres.password, postgres.mappedPort(5432))
    val signingConfig = SigningConfig("", sys.env("PRIVATE_KEY"))
    val appConfig = AppConfig(dbConfig, AuthSecretConfig("", ""), signingConfig)
    appConfig
  }

  test("get-video redirects to auth provider if the user is not logged in") {
    val appConfig = mock[AppConfig]
    val request = Request[IO](Method.GET, uri"/upload")
    val authConfig = new AuthConfig[IO]("").build()

    val route = UploadRoutes[IO](VideoService[IO](appConfig), _ => UUID.randomUUID().toString).routes()
    val contextBuilder: (Request[IO], Config) => Http4sWebContext[IO] = (_, _) => {
      Http4sWebContext.ioInstance(request, authConfig)
    }

    val authedProtectedPages: HttpRoutes[IO] =
      Session.sessionManagement[IO](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[IO](authConfig, contextBuilder))(route)
    authedProtectedPages.run(request).value.map(_.get.status).assertEquals(Status.Found)
  }
  override val containerDef: ContainerDef = PostgreSQLContainer.Def()
}
