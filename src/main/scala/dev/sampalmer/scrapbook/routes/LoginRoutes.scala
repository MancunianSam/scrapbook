package dev.sampalmer.scrapbook.routes

import cats.effect.Sync
import cats.implicits._
import cats.{Applicative, MonadThrow}
import dev.sampalmer.scrapbook.user.UserService.User
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{EntityDecoder, Headers, HttpRoutes, MediaType, Response, Status, UrlForm}
import org.http4s.twirl._
import tsec.authentication.SignedCookieAuthenticator
import tsec.mac.jca.HMACSHA256

import java.util.UUID

object LoginRoutes {
  case class UsernamePasswordCredentials(username: String, password: String)

  def login[F[_]: Sync] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "login" => Ok(html.login("bob"))
    }
  }

  def loginRoute[F[_] : Sync](
                                      auth: SignedCookieAuthenticator[F, UUID, User, HMACSHA256],
                                      checkPassword: UsernamePasswordCredentials => F[Option[User]])(implicit entityDecoder: EntityDecoder[F, UrlForm], monadThrow: MonadThrow[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case req@POST -> Root / "login" =>
        (for {
          user <- req.as[UrlForm]
          username: Option[String] = user.get("username").headOption
          password: Option[String] = user.get("password").headOption
          credentialsOpt: Option[UsernamePasswordCredentials] = Applicative[Option].map2[String, String, UsernamePasswordCredentials](username, password)(UsernamePasswordCredentials)
          credentials <- Sync[F].fromOption(credentialsOpt, new Exception("Missing username or password"))
          user <- checkPassword(credentials)
        } yield {
          user
        }).flatMap {
          case Some(user) =>
            val response = Response[F]().withStatus(Status.Found).withHeaders(Location(uri"/home"))
            auth.create(UUID.fromString(user.id.toString)).map(auth.embed(response, _))
          case None =>
            Sync[F].pure(
              Response[F](Status.Unauthorized)
                .withEntity(html.login("bob", error = true))
                .withHeaders(Headers(`Content-Type`(new MediaType("text", "html"))))
            )
        }
    }
  }
}
