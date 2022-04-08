package dev.sampalmer.scrapbook.user

import cats.{Eq, MonadError}
import org.http4s.circe.jsonEncoderOf
import org.http4s.{EntityEncoder, Response}

import java.util.UUID

object UserService {
  implicit def jsonStringListEncoder[F[_]]: EntityEncoder[F, List[String]] = jsonEncoderOf[F, List[String]]

  sealed case class Role(roleRepr: String)

  case class User(id: String, username: String, password: String, role: String)


}
