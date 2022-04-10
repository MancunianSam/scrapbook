package dev.sampalmer.scrapbook

import cats.data.OptionT
import cats.effect.IO
import cats.implicits.catsSyntaxTuple2Semigroupal
import com.dimafeng.testcontainers.munit.TestContainerForAll
import com.dimafeng.testcontainers.{ContainerDef, PostgreSQLContainer}
import dev.sampalmer.scrapbook.auth.AuthConfig
import dev.sampalmer.scrapbook.db.Db.transactionManager
import dev.sampalmer.scrapbook.routes.VideoRoutes
import dev.sampalmer.scrapbook.server.ScrapbookServer.{AppConfig, AuthSecretConfig, DbConfig, SigningConfig}
import dev.sampalmer.scrapbook.service.VideoService
import doobie.implicits._
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits.http4sLiteralsSyntax
import org.mockito.MockitoSugar.mock
import org.pac4j.core.config.Config
import org.pac4j.core.profile.CommonProfile
import org.pac4j.http4s._

import scala.concurrent.duration.DurationInt

class VideoSpec extends CatsEffectSuite with TestContainerForAll {

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
        val insert = sql"insert into videos (id, filename, notes) values ('e3ad334e-ac4f-47ec-8f3e-f36a79821c6d','test', 'test');".update.run
        (create, insert).mapN(_ + _).transact(xa).unsafeRunSync()
    }
    super.afterContainersStart(containers)
  }

  test("get-video returns the correct html") {
    withContainers {
      case postgres: PostgreSQLContainer =>
        val dbConfig = DbConfig(postgres.databaseName, postgres.username, postgres.password, postgres.mappedPort(5432))
        val signingConfig = SigningConfig("", sys.env("PRIVATE_KEY"))
        val appConfig = AppConfig(dbConfig, AuthSecretConfig("", ""), signingConfig)
        val request = Request[IO](Method.GET, uri"/get-video/e3ad334e-ac4f-47ec-8f3e-f36a79821c6d")

        val route = VideoRoutes[IO]().routes(VideoService[IO](appConfig))
        val contextRequest = ContextRequest(List[CommonProfile](), request)
        val body = responseBodyAsString(route.run(contextRequest))
        body.map(_.contains("<h1>mov_bbb.mp4</h1>")).assertEquals(true)
        body.map(_.contains("<video width=\"400\" controls>")).assertEquals(true)
    }
  }

  test("get-video redirects to auth provider if the user is not logged in") {
    val appConfig = mock[AppConfig]
    val request = Request[IO](Method.GET, uri"/get-video/e3ad334e-ac4f-47ec-8f3e-f36a79821c6d")
    val authConfig = new AuthConfig[IO]("").build()
    val contextBuilder: (Request[IO], Config) => Http4sWebContext[IO] = (_, _) => {
      Http4sWebContext.ioInstance(request, authConfig)
    }

    val route = VideoRoutes[IO]().routes(VideoService[IO](appConfig))
    val authedProtectedPages: HttpRoutes[IO] =
      Session.sessionManagement[IO](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[IO](authConfig, contextBuilder))(route)
    authedProtectedPages.run(request).value.map(_.get.status).assertEquals(Status.Found)
  }
  override val containerDef: ContainerDef = PostgreSQLContainer.Def()
}
