package dev.sampalmer.scrapbook.routes

import cats.data.OptionT
import cats.implicits._
import cats.{Applicative, MonadThrow}
import dev.sampalmer.scrapbook.domain.AuthService
import dev.sampalmer.scrapbook.tasks.TaskService
import dev.sampalmer.scrapbook.user.UserService.{Role, User}
import io.chrisdavenport.fuuid.FUUID
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, Response, Status}
import org.http4s.twirl._
import shapeless.tag
import shapeless.tag.@@
import tsec.authentication._
import tsec.authorization._
import tsec.mac.jca.HMACSHA256

import java.util.UUID

object TaskRoutes {
  trait TaskIdTag
  type TaskId = FUUID @@ TaskIdTag

  def tagFUUIDAsTaskId(id: FUUID): TaskId = tag[TaskIdTag][FUUID](id)

  def taskRoutes[F[_]: Applicative](taskService: TaskService[F])(implicit entityDecoder: EntityDecoder[F, String], monadThrow: MonadThrow[F]): AuthService[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    val basicRBAC = BasicRBAC[F, Role, User, AuthenticatedCookie[HMACSHA256, UUID]](Role.Administrator)
    TSecAuthService.withAuthorizationHandler[User, AuthenticatedCookie[HMACSHA256, UUID], F](basicRBAC)({
      case req@GET -> Root / "add-task" asAuthed user =>
        for {
          desc <- req.request.as[String]
          _ <- taskService.add(user.id, desc)
        } yield Response[F](Status.Ok)
    }, _ => {
      val status: F[Option[Response[F]]] = Ok(html.unauthorised("Authorisation error")).map(_.some)
      OptionT[F, Response[F]](status)
    })

  }
}
