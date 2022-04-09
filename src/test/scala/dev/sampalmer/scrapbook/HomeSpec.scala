package dev.sampalmer.scrapbook

import cats.effect.IO
import dev.sampalmer.scrapbook.routes.HomeRoute
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.implicits._

class HomeSpec extends CatsEffectSuite {

  test("login redirects on successful login") {
    val request = Request[IO](Method.GET, uri"/")
    val res = HomeRoute[IO]().routes().run(request)
    res.value.map(_.get.status.code).assertEquals(200)
  }
}
