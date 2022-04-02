package dev.sampalmer.scrapbook.utils

import cats.Applicative
import org.http4s.{Headers, MediaType, Response, Status}
import org.http4s.headers.`Content-Type`
import org.http4s.twirl._
import play.twirl.api.HtmlFormat

object Utils {
  def response[F[_]: Applicative](html: HtmlFormat.Appendable) = {
    Response[F](Status.Ok)
      .withEntity(html)
      .withHeaders(Headers(`Content-Type`(new MediaType("text", "html"))))
  }
}
