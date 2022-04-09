package dev.sampalmer.scrapbook.routes

import cats.effect.Async
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl._

trait HomeRoute[F[_]] {
  def routes(): HttpRoutes[F]
}
object HomeRoute {
  def apply[F[_]: Async]() = new HomeRoute[F] {
    val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    override def routes(): HttpRoutes[F] = {
      HttpRoutes.of[F] {
        case GET -> Root =>
          Ok(html.root("asas"))
      }
    }
  }
}
