package dev.sampalmer.scrapbook.routes

import cats.effect.kernel.Sync
import cats.implicits._
import cats.{Applicative, MonadThrow}
import dev.sampalmer.scrapbook.user.UserService.User
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io.{->, /, POST, Root}
import org.http4s.headers.Location
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.{EntityDecoder, HttpRoutes, Response, Status, UrlForm}
import org.http4s.twirl._
import tsec.authentication.SignedCookieAuthenticator
import tsec.mac.jca.HMACSHA256

import java.util.UUID

object LoginRoutes {
  case class UsernamePasswordCredentials(username: String, password: String)

  def loginRoute[F[_] : Sync](
                                      auth: SignedCookieAuthenticator[F, UUID, User, HMACSHA256],
                                      checkPassword: UsernamePasswordCredentials => F[Option[User]])(implicit entityDecoder: EntityDecoder[F, UrlForm], monadThrow: MonadThrow[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case req@POST -> Root / "login" =>
        (for {
          user <- req.as[UrlForm]
          username: Option[String] = user.get("username").headOption
          password: Option[String] = user.get("password").headOption
          credentialsOpt: Option[UsernamePasswordCredentials] = Applicative[Option].map2[String, String, UsernamePasswordCredentials](username, password)(UsernamePasswordCredentials)
          credentials <- Sync[F].fromOption(credentialsOpt, new Exception(""))
          user <- checkPassword(credentials)
        } yield {
          user
        }).flatMap {
          case Some(user) =>
            val response = Response[F]().withStatus(Status.Found).withHeaders(Location(uri"/home"))
            auth.create(user.id).map(auth.embed(response, _))
          case None =>
            val dsl = new Http4sDsl[F]{}
            import dsl._
            Ok(html.index("bob", error = true))
        }
    }
  }
}
