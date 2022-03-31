package dev.sampalmer.scrapbook.routes

import cats.effect.Sync
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl.htmlContentEncoder

object HomeRoutes {
  def home[F[_]: Sync] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "home" => Ok(html.index("bob"))
    }
  }

}
